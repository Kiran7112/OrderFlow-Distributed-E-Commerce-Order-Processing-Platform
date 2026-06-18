package com.orderflow.analytics.repository;

import com.orderflow.analytics.entity.FailureEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface FailureEventRepository extends JpaRepository<FailureEvent, UUID> {
    List<FailureEvent> findByEventType(String eventType);

    List<FailureEvent> findByOccurredAtGreaterThanOrderByOccurredAtDesc(LocalDateTime date);
}
