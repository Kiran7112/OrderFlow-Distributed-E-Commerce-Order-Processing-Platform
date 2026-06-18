package com.orderflow.inventory.kafka;

import com.orderflow.inventory.entity.ProcessedEvent;
import com.orderflow.inventory.entity.Stock;
import com.orderflow.inventory.event.OrderPlacedEvent;
import com.orderflow.inventory.repository.ProcessedEventRepository;
import com.orderflow.inventory.repository.StockRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class OrderPlacedConsumer {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private InventoryEventPublisher eventPublisher;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired(required = false)
    private RedisTemplate<String, Stock> redisTemplate;

    @KafkaListener(
            topics = "order-events",
            groupId = "inventory-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeOrderPlaced(OrderPlacedEvent event, Acknowledgment acknowledgment) {
        try {
            if (processedEventRepository.existsByEventId(event.getEventId())) {
                log.debug("Event already processed: {}", event.getEventId());
                acknowledgment.acknowledge();
                return;
            }

            log.info("Processing order.placed event: {}", event.getOrderId());

            boolean allReserved = true;
            StringBuilder reservationDetails = new StringBuilder();

            for (OrderPlacedEvent.OrderItemEvent item : event.getItems()) {
                Stock stock = getStockFromCacheOrDB(item.getProductId());

                if (stock == null) {
                    log.warn("Product not found: {}", item.getProductId());
                    allReserved = false;
                    reservationDetails.append("Product ").append(item.getProductId()).append(" not found. ");
                    break;
                }

                if (stock.getAvailableQty() < item.getQuantity()) {
                    log.warn("Insufficient stock for product {}: available={}, requested={}",
                            item.getProductId(), stock.getAvailableQty(), item.getQuantity());
                    allReserved = false;
                    reservationDetails.append("Insufficient stock for ").append(item.getProductId()).append(". ");
                    break;
                }

                stock.reserve(item.getQuantity());
                stockRepository.save(stock);

                if (redisTemplate != null) {
                    String cacheKey = "stock:" + item.getProductId();
                    redisTemplate.opsForValue().set(cacheKey, stock, 30, TimeUnit.SECONDS);
                    log.debug("Cached stock for product: {}", item.getProductId());
                }

                reservationDetails.append(item.getQuantity()).append("x ")
                        .append(item.getProductId()).append(" reserved. ");
            }

            if (allReserved) {
                eventPublisher.publishInventoryReserved(event.getOrderId(),
                        reservationDetails.toString());
                log.info("All items reserved for order: {}", event.getOrderId());
            } else {
                eventPublisher.publishInventoryInsufficient(event.getOrderId(),
                        reservationDetails.toString());
                log.info("Insufficient inventory for order: {}", event.getOrderId());
            }

            ProcessedEvent processed = ProcessedEvent.builder()
                    .eventId(event.getEventId())
                    .eventType("order.placed")
                    .orderId(event.getOrderId())
                    .build();
            processedEventRepository.save(processed);

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error consuming order.placed event: {}", event.getEventId(), e);
            throw new RuntimeException("Failed to process order.placed event", e);
        }
    }

    private Stock getStockFromCacheOrDB(java.util.UUID productId) {
        if (redisTemplate != null) {
            try {
                String cacheKey = "stock:" + productId;
                Stock cachedStock = redisTemplate.opsForValue().get(cacheKey);
                if (cachedStock != null) {
                    log.debug("Stock retrieved from cache for product: {}", productId);
                    return cachedStock;
                }
            } catch (Exception e) {
                log.warn("Failed to get stock from cache for product {}", productId, e);
            }
        }

        Optional<Stock> stockOpt = stockRepository.findByProductId(productId);
        return stockOpt.orElse(null);
    }
}
