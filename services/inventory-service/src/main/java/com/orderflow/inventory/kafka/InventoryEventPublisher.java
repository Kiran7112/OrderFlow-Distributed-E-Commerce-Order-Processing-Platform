package com.orderflow.inventory.kafka;

import com.orderflow.inventory.event.InventoryInsufficientEvent;
import com.orderflow.inventory.event.InventoryReservedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class InventoryEventPublisher {

    private static final String INVENTORY_EVENTS_TOPIC = "inventory-events";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void publishInventoryReserved(UUID orderId, String message) {
        try {
            InventoryReservedEvent event = InventoryReservedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .message(message)
                    .build();

            Message<InventoryReservedEvent> kafkaMessage = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, INVENTORY_EVENTS_TOPIC)
                    .setHeader("event_type", "inventory.reserved")
                    .setHeader("order_id", orderId.toString())
                    .build();

            kafkaTemplate.send(kafkaMessage);
            log.info("Published inventory.reserved event for order: {}", orderId);
        } catch (Exception e) {
            log.error("Failed to publish inventory.reserved event for order: {}", orderId, e);
            throw new RuntimeException("Failed to publish inventory event", e);
        }
    }

    public void publishInventoryInsufficient(UUID orderId, String message) {
        try {
            InventoryInsufficientEvent event = InventoryInsufficientEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .message(message)
                    .build();

            Message<InventoryInsufficientEvent> kafkaMessage = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, INVENTORY_EVENTS_TOPIC)
                    .setHeader("event_type", "inventory.insufficient")
                    .setHeader("order_id", orderId.toString())
                    .build();

            kafkaTemplate.send(kafkaMessage);
            log.info("Published inventory.insufficient event for order: {}", orderId);
        } catch (Exception e) {
            log.error("Failed to publish inventory.insufficient event for order: {}", orderId, e);
            throw new RuntimeException("Failed to publish inventory event", e);
        }
    }
}
