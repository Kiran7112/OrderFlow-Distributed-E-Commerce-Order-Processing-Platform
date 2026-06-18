package com.orderflow.payment.gateway;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
public class MockPaymentGateway {

    private static final Random random = new Random();

    @Value("${payment.mock.success-rate:0.90}")
    private double successRate;

    @Value("${payment.mock.min-delay-ms:100}")
    private long minDelayMs;

    @Value("${payment.mock.max-delay-ms:500}")
    private long maxDelayMs;

    public PaymentResponse processPayment(UUID orderId, BigDecimal amount, String currency) {
        try {
            // Simulate network latency
            long delay = minDelayMs + random.nextLong(maxDelayMs - minDelayMs + 1);
            Thread.sleep(delay);

            String gatewayRef = "MOCK-" + UUID.randomUUID().toString();
            double randomValue = random.nextDouble();

            if (randomValue < successRate) {
                log.info("Payment SUCCESS for order {}: amount={}, ref={}", orderId, amount, gatewayRef);
                return PaymentResponse.builder()
                        .gatewayRef(gatewayRef)
                        .status("SUCCESS")
                        .message("Payment processed successfully")
                        .build();
            } else {
                String failureReason = getRandomFailureReason();
                log.warn("Payment FAILED for order {}: reason={}", orderId, failureReason);
                return PaymentResponse.builder()
                        .gatewayRef(gatewayRef)
                        .status("FAILED")
                        .message("Payment declined: " + failureReason)
                        .failureReason(failureReason)
                        .build();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Payment gateway interrupted for order: {}", orderId, e);
            return PaymentResponse.builder()
                    .status("FAILED")
                    .message("Payment processing interrupted")
                    .failureReason("SYSTEM_ERROR")
                    .build();
        }
    }

    private String getRandomFailureReason() {
        String[] reasons = {
                "INSUFFICIENT_FUNDS",
                "CARD_DECLINED",
                "EXPIRED_CARD",
                "INVALID_CVV",
                "FRAUD_DETECTED",
                "GATEWAY_TIMEOUT",
                "PROCESSOR_ERROR"
        };
        return reasons[random.nextInt(reasons.length)];
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentResponse {
        private String gatewayRef;
        private String status;
        private String message;
        private String failureReason;

        public boolean isSuccess() {
            return "SUCCESS".equals(status);
        }
    }
}
