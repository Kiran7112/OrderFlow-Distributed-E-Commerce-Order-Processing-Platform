package com.orderflow.order.kafka;

import com.orderflow.order.entity.Order;
import com.orderflow.order.event.OrderPlacedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class OrderEventPublisher {

    private static final String ORDER_EVENTS_TOPIC = "order-events";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderPlaced(Order order) {
        try {
            OrderPlacedEvent event = OrderPlacedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(order.getId())
                    .customerId(order.getCustomerId())
                    .totalAmount(order.getTotalAmount())
                    .currency(order.getCurrency())
                    .items(order.getItems().stream()
                            .map(item -> OrderPlacedEvent.OrderItemEvent.builder()
                                    .productId(item.getProductId())
                                    .quantity(item.getQuantity())
                                    .unitPrice(item.getUnitPrice())
                                    .build())
                            .collect(Collectors.toList()))
                    .timestamp(LocalDateTime.now())
                    .build();

            Message<OrderPlacedEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, ORDER_EVENTS_TOPIC)
                    .setHeader("event_type", "order.placed")
                    .setHeader("order_id", order.getId().toString())
                    .build();

            kafkaTemplate.send(message);
            log.info("Published order.placed event for order: {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to publish order.placed event for order: {}", order.getId(), e);
            throw new RuntimeException("Failed to publish order event", e);
        }
    }
}
