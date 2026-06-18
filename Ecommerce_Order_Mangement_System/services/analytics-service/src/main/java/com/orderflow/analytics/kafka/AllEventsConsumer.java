package com.orderflow.analytics.kafka;

import com.orderflow.analytics.event.*;
import com.orderflow.analytics.service.AnalyticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AllEventsConsumer {

    @Autowired
    private AnalyticsService analyticsService;

    @KafkaListener(
            topics = "order-events",
            groupId = "analytics-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderEvent(OrderPlacedEvent event, Acknowledgment acknowledgment) {
        try {
            log.debug("Processing order.placed event for analytics: {}", event.getOrderId());
            analyticsService.recordOrderPlaced(event);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing order event: {}", event.getEventId(), e);
            throw new RuntimeException("Failed to process order event", e);
        }
    }

    @KafkaListener(
            topics = "inventory-events",
            groupId = "analytics-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeInventoryEvent(InventoryStatusEvent event, Acknowledgment acknowledgment) {
        try {
            log.debug("Processing inventory event for analytics: {}", event.getOrderId());
            analyticsService.recordInventoryEvent(event);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing inventory event: {}", event.getEventId(), e);
            throw new RuntimeException("Failed to process inventory event", e);
        }
    }

    @KafkaListener(
            topics = "payment-events",
            groupId = "analytics-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumePaymentEvent(PaymentStatusEvent event, Acknowledgment acknowledgment) {
        try {
            log.debug("Processing payment event for analytics: {}", event.getOrderId());
            analyticsService.recordPaymentEvent(event);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing payment event: {}", event.getEventId(), e);
            throw new RuntimeException("Failed to process payment event", e);
        }
    }

    @KafkaListener(
            topics = "shipping-events",
            groupId = "analytics-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeShippingEvent(ShippingStatusEvent event, Acknowledgment acknowledgment) {
        try {
            log.debug("Processing shipping event for analytics: {}", event.getOrderId());
            analyticsService.recordShippingEvent(event);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing shipping event: {}", event.getEventId(), e);
            throw new RuntimeException("Failed to process shipping event", e);
        }
    }
}
