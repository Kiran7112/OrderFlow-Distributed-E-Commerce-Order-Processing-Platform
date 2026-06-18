package com.orderflow.analytics.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "kafka_metrics", indexes = {
        @Index(name = "idx_consumer_group", columnList = "consumer_group"),
        @Index(name = "idx_measured_at", columnList = "measured_at"),
        @Index(name = "idx_kafka_metric_unique", columnList = "consumer_group,topic,measured_at", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KafkaMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String consumerGroup;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column
    private Long lag;

    @Column
    private Long offset;

    @Column
    private Long logEndOffset;

    @Column(nullable = false)
    private LocalDateTime measuredAt = LocalDateTime.now();
}
