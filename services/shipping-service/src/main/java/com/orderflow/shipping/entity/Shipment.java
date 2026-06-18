package com.orderflow.shipping.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shipments", indexes = {
        @Index(name = "idx_order_id", columnList = "order_id", unique = true),
        @Index(name = "idx_tracking_number", columnList = "tracking_number", unique = true),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID orderId;

    @Column(length = 50, unique = true)
    private String trackingNumber;

    @Column(length = 50)
    private String carrier;

    @Column(length = 100)
    private String carrierAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status = ShipmentStatus.CREATED;

    @Column
    private LocalDate estimatedDelivery;

    @Column
    private LocalDate actualDeliveryDate;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum ShipmentStatus {
        CREATED,
        IN_TRANSIT,
        OUT_FOR_DELIVERY,
        DELIVERED,
        RETURNED,
        FAILED
    }
}
