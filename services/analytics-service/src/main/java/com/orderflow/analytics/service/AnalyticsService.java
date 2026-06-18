package com.orderflow.analytics.service;

import com.orderflow.analytics.entity.*;
import com.orderflow.analytics.event.*;
import com.orderflow.analytics.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Service
public class AnalyticsService {

    @Autowired
    private OrderMetricRepository orderMetricRepository;

    @Autowired
    private RevenueRecordRepository revenueRecordRepository;

    @Autowired
    private FailureEventRepository failureEventRepository;

    @Autowired
    private KafkaMetricsRepository kafkaMetricsRepository;

    @Transactional
    public void recordOrderPlaced(OrderPlacedEvent event) {
        try {
            LocalDateTime hourStart = event.getTimestamp()
                    .withMinute(0).withSecond(0).withNano(0);

            OrderMetric metric = orderMetricRepository.findByMetricHour(hourStart)
                    .orElse(OrderMetric.builder().metricHour(hourStart).build());

            metric.setTotalOrders(metric.getTotalOrders() + 1);
            metric.setTotalRevenue(metric.getTotalRevenue().add(event.getTotalAmount()));
            metric.setAvgOrderValue(metric.getTotalRevenue()
                    .divide(new BigDecimal(metric.getTotalOrders()), BigDecimal.ROUND_HALF_UP));

            orderMetricRepository.save(metric);
            log.debug("Recorded order metric for: {}", hourStart);
        } catch (Exception e) {
            log.error("Error recording order metric: {}", event.getOrderId(), e);
        }
    }

    @Transactional
    public void recordPaymentEvent(PaymentStatusEvent event) {
        try {
            if ("payment.success".equals(event.getStatus())) {
                LocalDate today = LocalDate.now();
                RevenueRecord record = revenueRecordRepository.findByRevenueDate(today)
                        .orElse(RevenueRecord.builder().revenueDate(today).build());

                record.setPaymentSuccessCount(record.getPaymentSuccessCount() + 1);
                if (event.getAmount() != null) {
                    record.setTotalRevenue(record.getTotalRevenue().add(event.getAmount()));
                }
                revenueRecordRepository.save(record);
            } else if ("payment.failed".equals(event.getStatus())) {
                LocalDate today = LocalDate.now();
                RevenueRecord record = revenueRecordRepository.findByRevenueDate(today)
                        .orElse(RevenueRecord.builder().revenueDate(today).build());

                record.setPaymentFailureCount(record.getPaymentFailureCount() + 1);
                revenueRecordRepository.save(record);

                FailureEvent failure = FailureEvent.builder()
                        .eventType("PAYMENT_FAILED")
                        .failureReason(event.getFailureReason())
                        .orderId(event.getOrderId())
                        .affectedService("PAYMENT")
                        .occurredAt(LocalDateTime.now())
                        .build();
                failureEventRepository.save(failure);
            }
        } catch (Exception e) {
            log.error("Error recording payment metric: {}", event.getOrderId(), e);
        }
    }

    @Transactional
    public void recordInventoryEvent(InventoryStatusEvent event) {
        try {
            if ("inventory.insufficient".equals(event.getStatus())) {
                FailureEvent failure = FailureEvent.builder()
                        .eventType("INVENTORY_INSUFFICIENT")
                        .orderId(event.getOrderId())
                        .affectedService("INVENTORY")
                        .occurredAt(LocalDateTime.now())
                        .build();
                failureEventRepository.save(failure);
            }
        } catch (Exception e) {
            log.error("Error recording inventory metric: {}", event.getOrderId(), e);
        }
    }

    @Transactional
    public void recordShippingEvent(ShippingStatusEvent event) {
        try {
            if ("shipment.delivered".equals(event.getStatus())) {
                log.debug("Recorded shipment delivery for order: {}", event.getOrderId());
            }
        } catch (Exception e) {
            log.error("Error recording shipping metric: {}", event.getOrderId(), e);
        }
    }

    public List<OrderMetric> getOrderSummary(LocalDateTime startTime, LocalDateTime endTime) {
        return orderMetricRepository.findMetricsByDateRange(startTime, endTime);
    }

    public List<RevenueRecord> getRevenueSummary(LocalDate startDate, LocalDate endDate) {
        return revenueRecordRepository.findRecordsByDateRange(startDate, endDate);
    }

    public List<FailureEvent> getFailureRates() {
        return failureEventRepository.findByOccurredAtGreaterThanOrderByOccurredAtDesc(
                LocalDateTime.now().minusHours(24));
    }

    public long getRecentFailureCount(String eventType) {
        return failureEventRepository.findByEventType(eventType)
                .stream()
                .filter(e -> e.getOccurredAt().isAfter(LocalDateTime.now().minusHours(24)))
                .count();
    }
}
