package com.orderflow.analytics.repository;

import com.orderflow.analytics.entity.KafkaMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface KafkaMetricsRepository extends JpaRepository<KafkaMetrics, UUID> {
    List<KafkaMetrics> findByConsumerGroupOrderByMeasuredAtDesc(String consumerGroup);

    @Query("SELECT k FROM KafkaMetrics k WHERE k.consumerGroup = ?1 AND k.measuredAt >= ?2 ORDER BY k.measuredAt DESC")
    List<KafkaMetrics> findRecentMetrics(String consumerGroup, LocalDateTime since);
}
