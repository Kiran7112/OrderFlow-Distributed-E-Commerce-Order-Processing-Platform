package com.orderflow.payment.service;

import com.orderflow.payment.dto.TransactionDto;
import com.orderflow.payment.entity.Transaction;
import com.orderflow.payment.gateway.MockPaymentGateway;
import com.orderflow.payment.kafka.PaymentEventPublisher;
import com.orderflow.payment.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class PaymentService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private MockPaymentGateway paymentGateway;

    @Autowired
    private PaymentEventPublisher eventPublisher;

    @Transactional
    public TransactionDto processPayment(UUID orderId) {
        log.info("Processing payment for order: {}", orderId);

        Optional<Transaction> existingOpt = transactionRepository.findByOrderId(orderId);
        if (existingOpt.isPresent()) {
            Transaction existing = existingOpt.get();
            if (existing.getStatus() != Transaction.PaymentStatus.PENDING) {
                log.warn("Payment already processed for order {}: status={}", orderId, existing.getStatus());
                return convertToDto(existing);
            }
        }

        Transaction transaction = Transaction.builder()
                .orderId(orderId)
                .amount(BigDecimal.ZERO)
                .status(Transaction.PaymentStatus.PENDING)
                .gatewayName("MOCK")
                .build();

        Transaction saved = transactionRepository.save(transaction);

        MockPaymentGateway.PaymentResponse response = paymentGateway.processPayment(
                orderId, BigDecimal.ZERO, "USD");

        if (response.isSuccess()) {
            saved.setStatus(Transaction.PaymentStatus.SUCCESS);
            saved.setGatewayRef(response.getGatewayRef());
            Transaction updated = transactionRepository.save(saved);

            eventPublisher.publishPaymentSuccess(orderId, updated.getId().toString(),
                    updated.getAmount(), updated.getCurrency());

            log.info("Payment SUCCESS for order: {}", orderId);
            return convertToDto(updated);
        } else {
            saved.setStatus(Transaction.PaymentStatus.FAILED);
            saved.setGatewayRef(response.getGatewayRef());
            Transaction updated = transactionRepository.save(saved);

            eventPublisher.publishPaymentFailed(orderId, updated.getId().toString(),
                    response.getFailureReason());

            log.warn("Payment FAILED for order: {}, reason: {}", orderId, response.getFailureReason());
            return convertToDto(updated);
        }
    }

    public TransactionDto getPaymentByOrderId(UUID orderId) {
        log.debug("Fetching payment for order: {}", orderId);
        Transaction transaction = transactionRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));
        return convertToDto(transaction);
    }

    private TransactionDto convertToDto(Transaction transaction) {
        return TransactionDto.builder()
                .id(transaction.getId())
                .orderId(transaction.getOrderId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .gatewayRef(transaction.getGatewayRef())
                .gatewayName(transaction.getGatewayName())
                .status(transaction.getStatus().name())
                .paymentMethod(transaction.getPaymentMethod())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }
}
