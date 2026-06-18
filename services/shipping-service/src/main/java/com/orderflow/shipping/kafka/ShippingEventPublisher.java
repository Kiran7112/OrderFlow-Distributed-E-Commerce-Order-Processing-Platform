package com.orderflow.shipping.kafka;

import com.orderflow.shipping.event.ShipmentCreatedEvent;
import com.orderflow.shipping.event.ShipmentDeliveredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Component
public class ShippingEventPublisher {

    private static final String SHIPPING_EVENTS_TOPIC = "shipping-events";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void publishShipmentCreated(UUID orderId, String shipmentId, String trackingNumber,
                                      String carrier, LocalDate estimatedDelivery) {
        try {
            ShipmentCreatedEvent event = ShipmentCreatedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .shipmentId(shipmentId)
                    .trackingNumber(trackingNumber)
                    .carrier(carrier)
                    .estimatedDelivery(estimatedDelivery)
                    .message("Shipment created and ready for dispatch")
                    .build();

            Message<ShipmentCreatedEvent> kafkaMessage = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, SHIPPING_EVENTS_TOPIC)
                    .setHeader("event_type", "shipment.created")
                    .setHeader("order_id", orderId.toString())
                    .build();

            kafkaTemplate.send(kafkaMessage);
            log.info("Published shipment.created event for order: {}, tracking: {}", orderId, trackingNumber);
        } catch (Exception e) {
            log.error("Failed to publish shipment.created event for order: {}", orderId, e);
            throw new RuntimeException("Failed to publish shipment event", e);
        }
    }

    public void publishShipmentDelivered(UUID orderId, String shipmentId, String trackingNumber,
                                        LocalDate actualDeliveryDate) {
        try {
            ShipmentDeliveredEvent event = ShipmentDeliveredEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .shipmentId(shipmentId)
                    .trackingNumber(trackingNumber)
                    .actualDeliveryDate(actualDeliveryDate)
                    .message("Order delivered successfully")
                    .build();

            Message<ShipmentDeliveredEvent> kafkaMessage = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, SHIPPING_EVENTS_TOPIC)
                    .setHeader("event_type", "shipment.delivered")
                    .setHeader("order_id", orderId.toString())
                    .build();

            kafkaTemplate.send(kafkaMessage);
            log.info("Published shipment.delivered event for order: {}, tracking: {}", orderId, trackingNumber);
        } catch (Exception e) {
            log.error("Failed to publish shipment.delivered event for order: {}", orderId, e);
            throw new RuntimeException("Failed to publish shipment event", e);
        }
    }
}
