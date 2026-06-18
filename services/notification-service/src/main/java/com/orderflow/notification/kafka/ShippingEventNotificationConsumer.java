package com.orderflow.notification.kafka;

import com.orderflow.notification.event.ShippingStatusEvent;
import com.orderflow.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ShippingEventNotificationConsumer {

    @Autowired
    private NotificationService notificationService;

    @KafkaListener(
            topics = "shipping-events",
            groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeShippingEvent(ShippingStatusEvent event, Acknowledgment acknowledgment) {
        try {
            log.info("Processing shipping notification for order: {}, status: {}", event.getOrderId(), event.getStatus());

            if ("shipment.created".equals(event.getStatus())) {
                notificationService.sendShipmentCreatedNotification(
                        event.getOrderId(),
                        event.getTrackingNumber(),
                        event.getEstimatedDelivery()
                );
                log.info("Shipment created notification sent for order: {}", event.getOrderId());
            } else if ("shipment.delivered".equals(event.getStatus())) {
                notificationService.sendShipmentDeliveredNotification(
                        event.getOrderId(),
                        event.getTrackingNumber(),
                        event.getActualDeliveryDate()
                );
                log.info("Shipment delivered notification sent for order: {}", event.getOrderId());
            }

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing shipping notification: {}", event.getEventId(), e);
            throw new RuntimeException("Failed to process shipping notification", e);
        }
    }
}
