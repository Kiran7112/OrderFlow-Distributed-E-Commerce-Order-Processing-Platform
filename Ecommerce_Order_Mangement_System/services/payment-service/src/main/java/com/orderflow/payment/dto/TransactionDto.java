package com.orderflow.payment.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private UUID orderId;
    private BigDecimal amount;
    private String currency;
    private String gatewayRef;
    private String gatewayName;
    private String status;
    private String paymentMethod;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
