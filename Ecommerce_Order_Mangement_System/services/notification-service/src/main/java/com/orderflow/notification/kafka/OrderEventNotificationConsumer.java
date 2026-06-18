package com.orderflow.notification.kafka;

import com.orderflow.notification.event.OrderPlacedEvent;
import com.orderflow.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEventNotificationConsumer {

    @Autowired
    private NotificationService notificationService;

    @KafkaListener(
            topics = "order-events",
            groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderPlaced(OrderPlacedEvent event, Acknowledgment acknowledgment) {
        try {
            log.info("Processing order.placed notification for order: {}", event.getOrderId());

            notificationService.sendOrderPlacedNotification(
                    event.getOrderId(),
                    event.getCustomerId(),
                    event.getTotalAmount(),
                    event.getCurrency()
            );

            log.info("Order placed notification sent for order: {}", event.getOrderId());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing order.placed notification: {}", event.getEventId(), e);
            throw new RuntimeException("Failed to process order notification", e);
        }
    }
}
