package com.orderflow.analytics.controller;

import com.orderflow.analytics.entity.FailureEvent;
import com.orderflow.analytics.entity.OrderMetric;
import com.orderflow.analytics.entity.RevenueRecord;
import com.orderflow.analytics.kafka.KafkaAdminService;
import com.orderflow.analytics.service.AnalyticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private KafkaAdminService kafkaAdminService;

    @GetMapping("/orders/summary")
    public ResponseEntity<Map<String, Object>> getOrdersSummary(
            @RequestParam(defaultValue = "0") int hoursBack) {
        log.debug("GET /api/analytics/orders/summary - Fetching order metrics");
        try {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusHours(hoursBack > 0 ? hoursBack : 24);

            List<OrderMetric> metrics = analyticsService.getOrderSummary(startTime, endTime);

            Map<String, Object> response = new HashMap<>();
            response.put("period", String.format("%s to %s", startTime, endTime));
            response.put("metrics", metrics);
            response.put("count", metrics.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching order summary", e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/revenue/daily")
    public ResponseEntity<Map<String, Object>> getRevenueSummary(
            @RequestParam(defaultValue = "30") int days) {
        log.debug("GET /api/analytics/revenue/daily - Fetching revenue metrics");
        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(days);

            List<RevenueRecord> records = analyticsService.getRevenueSummary(startDate, endDate);

            Map<String, Object> response = new HashMap<>();
            response.put("period", String.format("%s to %s", startDate, endDate));
            response.put("records", records);
            response.put("count", records.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching revenue summary", e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/failures")
    public ResponseEntity<Map<String, Object>> getFailureRates() {
        log.debug("GET /api/analytics/failures - Fetching failure metrics");
        try {
            List<FailureEvent> failures = analyticsService.getFailureRates();

            Map<String, Object> response = new HashMap<>();
            response.put("totalFailures", failures.size());
            response.put("paymentFailures", analyticsService.getRecentFailureCount("PAYMENT_FAILED"));
            response.put("inventoryFailures", analyticsService.getRecentFailureCount("INVENTORY_INSUFFICIENT"));
            response.put("events", failures);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching failure rates", e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/kafka/consumer-lag")
    public ResponseEntity<Map<String, Object>> getKafkaConsumerLag() {
        log.debug("GET /api/analytics/kafka/consumer-lag - Fetching Kafka metrics");
        try {
            Map<String, KafkaAdminService.ConsumerGroupLag> lags =
                    kafkaAdminService.getConsumerGroupLags("analytics-service-group");

            Map<String, Object> response = new HashMap<>();
            response.put("consumerGroup", "analytics-service-group");
            response.put("totalLag", lags.values().stream()
                    .mapToLong(lag -> lag.lag)
                    .sum());
            response.put("metrics", lags);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching Kafka consumer lag", e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Analytics Service is healthy");
    }
}
