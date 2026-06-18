package com.orderflow.order.service;

import com.orderflow.order.dto.OrderItemDto;
import com.orderflow.order.dto.OrderRequest;
import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.entity.Order;
import com.orderflow.order.entity.OrderItem;
import com.orderflow.order.kafka.OrderEventPublisher;
import com.orderflow.order.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderEventPublisher eventPublisher;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());

        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .status(Order.OrderStatus.PLACED)
                .currency("USD")
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItemDto itemDto : request.getItems()) {
            OrderItem item = OrderItem.builder()
                    .productId(itemDto.getProductId())
                    .quantity(itemDto.getQuantity())
                    .unitPrice(itemDto.getUnitPrice())
                    .build();
            order.addItem(item);
            totalAmount = totalAmount.add(item.getLineTotal());
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);
        log.info("Order created with ID: {}", savedOrder.getId());

        eventPublisher.publishOrderPlaced(savedOrder);

        return OrderResponse.fromEntity(savedOrder);
    }

    public OrderResponse getOrderById(UUID orderId) {
        log.debug("Fetching order: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        return OrderResponse.fromEntity(order);
    }

    public List<OrderResponse> getOrdersByCustomerId(UUID customerId) {
        log.debug("Fetching orders for customer: {}", customerId);
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId) {
        log.info("Cancelling order: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (order.getStatus() == Order.OrderStatus.SHIPPED
                || order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new RuntimeException("Cannot cancel order that has already shipped");
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        Order updated = orderRepository.save(order);
        log.info("Order {} cancelled", orderId);

        return OrderResponse.fromEntity(updated);
    }

    public List<OrderResponse> getOrdersByStatus(String status) {
        log.debug("Fetching orders with status: {}", status);
        Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
        return orderRepository.findByStatus(orderStatus)
                .stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
