package com.orderflow.order.controller;

import com.orderflow.order.dto.OrderRequest;
import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*", maxAge = 3600)
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        log.info("POST /api/orders - Creating order for customer: {}", request.getCustomerId());
        try {
            OrderResponse response = orderService.createOrder(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating order", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID orderId) {
        log.debug("GET /api/orders/{} - Fetching order", orderId);
        try {
            OrderResponse response = orderService.getOrderById(orderId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("Order not found: {}", orderId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomerId(@PathVariable UUID customerId) {
        log.debug("GET /api/orders/customer/{} - Fetching orders", customerId);
        List<OrderResponse> responses = orderService.getOrdersByCustomerId(customerId);
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable UUID orderId) {
        log.info("PATCH /api/orders/{}/cancel - Cancelling order", orderId);
        try {
            OrderResponse response = orderService.cancelOrder(orderId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("Error cancelling order {}: {}", orderId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(@PathVariable String status) {
        log.debug("GET /api/orders/status/{} - Fetching orders", status);
        try {
            List<OrderResponse> responses = orderService.getOrdersByStatus(status);
            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid status: {}", status);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Order Service is healthy");
    }
}
