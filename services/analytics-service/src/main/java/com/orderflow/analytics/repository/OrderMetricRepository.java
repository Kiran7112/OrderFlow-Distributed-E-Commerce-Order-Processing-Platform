package com.orderflow.analytics.repository;

import com.orderflow.analytics.entity.OrderMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderMetricRepository extends JpaRepository<OrderMetric, UUID> {
    Optional<OrderMetric> findByMetricHour(LocalDateTime metricHour);

    @Query("SELECT o FROM OrderMetric o WHERE o.metricHour >= ?1 AND o.metricHour < ?2 ORDER BY o.metricHour DESC")
    List<OrderMetric> findMetricsByDateRange(LocalDateTime startTime, LocalDateTime endTime);
}
