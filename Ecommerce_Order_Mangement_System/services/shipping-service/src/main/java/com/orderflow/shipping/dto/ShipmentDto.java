package com.orderflow.shipping.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private UUID orderId;
    private String trackingNumber;
    private String carrier;
    private String status;
    private LocalDate estimatedDelivery;
    private LocalDate actualDeliveryDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
