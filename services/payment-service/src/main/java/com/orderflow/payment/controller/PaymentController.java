package com.orderflow.payment.controller;

import com.orderflow.payment.dto.TransactionDto;
import com.orderflow.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping("/order/{orderId}")
    public ResponseEntity<TransactionDto> getPaymentByOrderId(@PathVariable UUID orderId) {
        log.debug("GET /api/payments/order/{} - Fetching payment", orderId);
        try {
            TransactionDto payment = paymentService.getPaymentByOrderId(orderId);
            return ResponseEntity.ok(payment);
        } catch (RuntimeException e) {
            log.warn("Payment not found for order: {}", orderId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Payment Service is healthy");
    }
}
