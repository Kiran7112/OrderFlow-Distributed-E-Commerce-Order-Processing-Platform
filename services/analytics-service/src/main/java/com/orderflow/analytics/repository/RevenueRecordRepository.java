package com.orderflow.analytics.repository;

import com.orderflow.analytics.entity.RevenueRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RevenueRecordRepository extends JpaRepository<RevenueRecord, UUID> {
    Optional<RevenueRecord> findByRevenueDate(LocalDate date);

    @Query("SELECT r FROM RevenueRecord r WHERE r.revenueDate >= ?1 AND r.revenueDate <= ?2 ORDER BY r.revenueDate DESC")
    List<RevenueRecord> findRecordsByDateRange(LocalDate startDate, LocalDate endDate);
}
