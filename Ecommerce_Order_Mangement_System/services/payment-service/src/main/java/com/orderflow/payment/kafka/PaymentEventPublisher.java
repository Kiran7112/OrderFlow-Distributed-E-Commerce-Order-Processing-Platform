package com.orderflow.payment.kafka;

import com.orderflow.payment.event.PaymentFailedEvent;
import com.orderflow.payment.event.PaymentSuccessEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
public class PaymentEventPublisher {

    private static final String PAYMENT_EVENTS_TOPIC = "payment-events";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPaymentSuccess(UUID orderId, String transactionId, BigDecimal amount, String currency) {
        try {
            PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .transactionId(transactionId)
                    .amount(amount)
                    .currency(currency)
                    .message("Payment processed successfully")
                    .build();

            Message<PaymentSuccessEvent> kafkaMessage = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, PAYMENT_EVENTS_TOPIC)
                    .setHeader("event_type", "payment.success")
                    .setHeader("order_id", orderId.toString())
                    .build();

            kafkaTemplate.send(kafkaMessage);
            log.info("Published payment.success event for order: {}", orderId);
        } catch (Exception e) {
            log.error("Failed to publish payment.success event for order: {}", orderId, e);
            throw new RuntimeException("Failed to publish payment event", e);
        }
    }

    public void publishPaymentFailed(UUID orderId, String transactionId, String failureReason) {
        try {
            PaymentFailedEvent event = PaymentFailedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .transactionId(transactionId)
                    .failureReason(failureReason)
                    .message("Payment failed: " + failureReason)
                    .build();

            Message<PaymentFailedEvent> kafkaMessage = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, PAYMENT_EVENTS_TOPIC)
                    .setHeader("event_type", "payment.failed")
                    .setHeader("order_id", orderId.toString())
                    .build();

            kafkaTemplate.send(kafkaMessage);
            log.info("Published payment.failed event for order: {}", orderId);
        } catch (Exception e) {
            log.error("Failed to publish payment.failed event for order: {}", orderId, e);
            throw new RuntimeException("Failed to publish payment event", e);
        }
    }
}
