package com.orderflow.payment.kafka;

import com.orderflow.payment.entity.ProcessedEvent;
import com.orderflow.payment.event.InventoryReservedEvent;
import com.orderflow.payment.repository.ProcessedEventRepository;
import com.orderflow.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class InventoryReservedConsumer {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @KafkaListener(
            topics = "inventory-events",
            groupId = "payment-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeInventoryReserved(InventoryReservedEvent event, Acknowledgment acknowledgment) {
        try {
            if (processedEventRepository.existsByEventId(event.getEventId())) {
                log.debug("Event already processed: {}", event.getEventId());
                acknowledgment.acknowledge();
                return;
            }

            if ("inventory.reserved".equals(event.getStatus())) {
                log.info("Processing payment for order: {}", event.getOrderId());
                paymentService.processPayment(event.getOrderId());
            } else if ("inventory.insufficient".equals(event.getStatus())) {
                log.info("Inventory insufficient for order: {}, skipping payment", event.getOrderId());
            }

            ProcessedEvent processed = ProcessedEvent.builder()
                    .eventId(event.getEventId())
                    .eventType("inventory_reserved")
                    .orderId(event.getOrderId())
                    .build();
            processedEventRepository.save(processed);

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error consuming inventory reserved event: {}", event.getEventId(), e);
            throw new RuntimeException("Failed to process inventory reserved event", e);
        }
    }
}
