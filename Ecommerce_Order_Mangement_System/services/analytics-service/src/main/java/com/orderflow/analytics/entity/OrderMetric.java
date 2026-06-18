package com.orderflow.analytics.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_metrics", indexes = {
        @Index(name = "idx_metric_hour", columnList = "metric_hour")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private LocalDateTime metricHour;

    @Column
    private Integer totalOrders = 0;

    @Column
    private Integer confirmedOrders = 0;

    @Column
    private Integer shippedOrders = 0;

    @Column
    private Integer deliveredOrders = 0;

    @Column
    private Integer cancelledOrders = 0;

    @Column(precision = 19, scale = 2)
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal avgOrderValue = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
