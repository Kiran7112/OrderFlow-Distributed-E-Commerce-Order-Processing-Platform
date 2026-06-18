package com.orderflow.order.kafka;

import com.orderflow.order.entity.Order;
import com.orderflow.order.entity.ProcessedEvent;
import com.orderflow.order.event.InventoryStatusEvent;
import com.orderflow.order.event.PaymentStatusEvent;
import com.orderflow.order.repository.OrderRepository;
import com.orderflow.order.repository.ProcessedEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class OrderStatusUpdateConsumer {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @KafkaListener(
            topics = "inventory-events",
            groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeInventoryEvent(InventoryStatusEvent event, Acknowledgment acknowledgment) {
        try {
            if (processedEventRepository.existsByEventId(event.getEventId())) {
                log.debug("Event already processed: {}", event.getEventId());
                acknowledgment.acknowledge();
                return;
            }

            Order order = orderRepository.findById(event.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + event.getOrderId()));

            if ("inventory.reserved".equals(event.getStatus())) {
                order.setStatus(Order.OrderStatus.CONFIRMED);
                log.info("Order {} status updated to CONFIRMED", event.getOrderId());
            } else if ("inventory.insufficient".equals(event.getStatus())) {
                order.setStatus(Order.OrderStatus.CANCELLED);
                log.info("Order {} cancelled due to insufficient inventory", event.getOrderId());
            }

            orderRepository.save(order);

            ProcessedEvent processed = ProcessedEvent.builder()
                    .eventId(event.getEventId())
                    .eventType("inventory_status")
                    .orderId(event.getOrderId())
                    .build();
            processedEventRepository.save(processed);

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error consuming inventory event: {}", event.getEventId(), e);
            throw new RuntimeException("Failed to process inventory event", e);
        }
    }

    @KafkaListener(
            topics = "payment-events",
            groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumePaymentEvent(PaymentStatusEvent event, Acknowledgment acknowledgment) {
        try {
            if (processedEventRepository.existsByEventId(event.getEventId())) {
                log.debug("Event already processed: {}", event.getEventId());
                acknowledgment.acknowledge();
                return;
            }

            Order order = orderRepository.findById(event.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + event.getOrderId()));

            if ("payment.success".equals(event.getStatus())) {
                order.setStatus(Order.OrderStatus.SHIPPED);
                log.info("Order {} status updated to SHIPPED", event.getOrderId());
            } else if ("payment.failed".equals(event.getStatus())) {
                order.setStatus(Order.OrderStatus.PAYMENT_FAILED);
                log.info("Order {} marked as PAYMENT_FAILED", event.getOrderId());
            }

            orderRepository.save(order);

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
