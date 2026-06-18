package com.orderflow.inventory.service;

import com.orderflow.inventory.dto.ProductDto;
import com.orderflow.inventory.dto.StockDto;
import com.orderflow.inventory.entity.Product;
import com.orderflow.inventory.entity.Stock;
import com.orderflow.inventory.repository.ProductRepository;
import com.orderflow.inventory.repository.StockRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InventoryService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired(required = false)
    private RedisTemplate<String, Stock> redisTemplate;

    @Transactional(readOnly = true)
    public StockDto getStockByProductId(UUID productId) {
        log.debug("Fetching stock for product: {}", productId);

        Stock stock = getStockFromCacheOrDB(productId);
        if (stock == null) {
            throw new RuntimeException("Stock not found for product: " + productId);
        }

        return convertToDto(stock);
    }

    @Transactional
    public void updateStock(UUID productId, Integer availableQty, Integer reorderLevel) {
        log.info("Updating stock for product: {}, qty: {}", productId, availableQty);

        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Stock not found for product: " + productId));

        stock.setAvailableQty(availableQty);
        stock.setReorderLevel(reorderLevel);
        Stock updated = stockRepository.save(stock);

        if (redisTemplate != null) {
            String cacheKey = "stock:" + productId;
            redisTemplate.opsForValue().set(cacheKey, updated, 30, TimeUnit.SECONDS);
            log.debug("Cache updated for product: {}", productId);
        }
    }

    @Transactional(readOnly = true)
    public List<StockDto> getLowStockItems() {
        log.debug("Fetching low stock items");
        return stockRepository.findLowStockItems()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductDto createProduct(String name, String description, BigDecimal price,
                                    String category, String sku) {
        log.info("Creating product: {}", sku);

        Product product = Product.builder()
                .name(name)
                .description(description)
                .price(price)
                .category(category)
                .sku(sku)
                .isActive(true)
                .build();

        Product saved = productRepository.save(product);

        Stock stock = Stock.builder()
                .product(saved)
                .availableQty(0)
                .reservedQty(0)
                .reorderLevel(10)
                .build();
        stockRepository.save(stock);

        return convertProductToDto(saved);
    }

    public ProductDto getProductById(UUID productId) {
        log.debug("Fetching product: {}", productId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        return convertProductToDto(product);
    }

    public List<ProductDto> getProductsByCategory(String category) {
        log.debug("Fetching products by category: {}", category);
        return productRepository.findByCategory(category)
                .stream()
                .map(this::convertProductToDto)
                .collect(Collectors.toList());
    }

    private Stock getStockFromCacheOrDB(UUID productId) {
        if (redisTemplate != null) {
            try {
                String cacheKey = "stock:" + productId;
                Stock cachedStock = redisTemplate.opsForValue().get(cacheKey);
                if (cachedStock != null) {
                    log.debug("Stock retrieved from cache: {}", productId);
                    return cachedStock;
                }
            } catch (Exception e) {
                log.warn("Failed to get stock from cache for product {}", productId, e);
            }
        }

        Optional<Stock> stock = stockRepository.findByProductId(productId);
        return stock.orElse(null);
    }

    private StockDto convertToDto(Stock stock) {
        return StockDto.builder()
                .productId(stock.getProduct().getId())
                .availableQty(stock.getAvailableQty())
                .reservedQty(stock.getReservedQty())
                .totalQty(stock.getTotalQty())
                .reorderLevel(stock.getReorderLevel())
                .isLowStock(stock.isLowStock())
                .lastUpdated(stock.getLastUpdated())
                .build();
    }

    private ProductDto convertProductToDto(Product product) {
        return ProductDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .category(product.getCategory())
                .sku(product.getSku())
                .isActive(product.getIsActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
