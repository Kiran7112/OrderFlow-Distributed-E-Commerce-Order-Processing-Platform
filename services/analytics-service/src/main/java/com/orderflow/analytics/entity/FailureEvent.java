package com.orderflow.analytics.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "failure_events", indexes = {
        @Index(name = "idx_event_type", columnList = "event_type"),
        @Index(name = "idx_occurred_at", columnList = "occurred_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailureEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(length = 255)
    private String failureReason;

    @Column
    private UUID orderId;

    @Column(length = 50)
    private String affectedService;

    @Column(nullable = false)
    private LocalDateTime occurredAt = LocalDateTime.now();

    @Column
    private Boolean resolved = false;
}
