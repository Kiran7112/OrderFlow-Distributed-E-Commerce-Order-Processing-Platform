package com.orderflow.notification.kafka;

import com.orderflow.notification.event.PaymentStatusEvent;
import com.orderflow.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentEventNotificationConsumer {

    @Autowired
    private NotificationService notificationService;

    @KafkaListener(
            topics = "payment-events",
            groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumePaymentEvent(PaymentStatusEvent event, Acknowledgment acknowledgment) {
        try {
            log.info("Processing payment notification for order: {}, status: {}", event.getOrderId(), event.getStatus());

            if ("payment.success".equals(event.getStatus())) {
                notificationService.sendPaymentSuccessNotification(
                        event.getOrderId(),
                        event.getAmount(),
                        event.getCurrency()
                );
                log.info("Payment success notification sent for order: {}", event.getOrderId());
            } else if ("payment.failed".equals(event.getStatus())) {
                notificationService.sendPaymentFailedNotification(
                        event.getOrderId(),
                        event.getFailureReason()
                );
                log.info("Payment failed notification sent for order: {}", event.getOrderId());
            }

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing payment notification: {}", event.getEventId(), e);
            throw new RuntimeException("Failed to process payment notification", e);
        }
    }
}
