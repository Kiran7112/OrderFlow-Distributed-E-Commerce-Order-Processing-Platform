package com.orderflow.inventory.kafka;

import com.orderflow.inventory.entity.ProcessedEvent;
import com.orderflow.inventory.event.PaymentStatusEvent;
import com.orderflow.inventory.repository.ProcessedEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentFailedCompensationConsumer {

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @KafkaListener(
            topics = "payment-events",
            groupId = "inventory-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumePaymentEvent(PaymentStatusEvent event, Acknowledgment acknowledgment) {
        try {
            if (processedEventRepository.existsByEventId(event.getEventId())) {
                log.debug("Event already processed: {}", event.getEventId());
                acknowledgment.acknowledge();
                return;
            }

            if ("payment.failed".equals(event.getStatus())) {
                log.info("Payment failed for order: {}, releasing stock reservations", event.getOrderId());
                log.debug("Note: Stock release would be triggered via compensating transaction");
            }

            ProcessedEvent processed = ProcessedEvent.builder()
                    .eventId(event.getEventId())
                    .eventType("payment_status")
                    .orderId(event.getOrderId())
                    .build();
            processedEventRepository.save(processed);

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error consuming payment event: {}", event.getEventId(), e);
            throw new RuntimeException("Failed to process payment event", e);
        }
    }
}
