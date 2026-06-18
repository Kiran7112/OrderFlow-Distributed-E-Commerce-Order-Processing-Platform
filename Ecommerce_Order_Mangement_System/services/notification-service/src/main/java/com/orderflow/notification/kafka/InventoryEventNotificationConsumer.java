package com.orderflow.notification.kafka;

import com.orderflow.notification.event.InventoryStatusEvent;
import com.orderflow.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InventoryEventNotificationConsumer {

    @Autowired
    private NotificationService notificationService;

    @KafkaListener(
            topics = "inventory-events",
            groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeInventoryEvent(InventoryStatusEvent event, Acknowledgment acknowledgment) {
        try {
            log.info("Processing inventory notification for order: {}, status: {}", event.getOrderId(), event.getStatus());

            if ("inventory.insufficient".equals(event.getStatus())) {
                notificationService.sendInventoryInsufficientNotification(event.getOrderId());
                log.info("Inventory insufficient notification sent for order: {}", event.getOrderId());
            }

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing inventory notification: {}", event.getEventId(), e);
            throw new RuntimeException("Failed to process inventory notification", e);
        }
    }
}
