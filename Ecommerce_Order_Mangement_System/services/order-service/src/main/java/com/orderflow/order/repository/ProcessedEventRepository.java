package com.orderflow.order.repository;

import com.orderflow.order.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
    Optional<ProcessedEvent> findByEventId(String eventId);
    boolean existsByEventId(String eventId);
}
