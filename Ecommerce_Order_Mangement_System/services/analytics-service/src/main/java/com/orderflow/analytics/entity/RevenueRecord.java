package com.orderflow.analytics.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "revenue_records", indexes = {
        @Index(name = "idx_revenue_date", columnList = "revenue_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private LocalDate revenueDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalRevenue;

    @Column(nullable = false)
    private Integer totalOrders;

    @Column(precision = 19, scale = 2)
    private BigDecimal avgOrderValue;

    @Column
    private Integer paymentSuccessCount = 0;

    @Column
    private Integer paymentFailureCount = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
