package com.orderflow.shipping.kafka;

import com.orderflow.shipping.entity.ProcessedEvent;
import com.orderflow.shipping.event.PaymentStatusEvent;
import com.orderflow.shipping.repository.ProcessedEventRepository;
import com.orderflow.shipping.service.ShippingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class PaymentSuccessConsumer {

    @Autowired
    private ShippingService shippingService;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @KafkaListener(
            topics = "payment-events",
            groupId = "shipping-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumePaymentEvent(PaymentStatusEvent event, Acknowledgment acknowledgment) {
        try {
            if (processedEventRepository.existsByEventId(event.getEventId())) {
                log.debug("Event already processed: {}", event.getEventId());
                acknowledgment.acknowledge();
                return;
            }

            if ("payment.success".equals(event.getStatus())) {
                log.info("Processing shipment creation for order: {}", event.getOrderId());
                shippingService.createShipment(event.getOrderId());
            } else if ("payment.failed".equals(event.getStatus())) {
                log.info("Payment failed for order: {}, skipping shipment", event.getOrderId());
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
