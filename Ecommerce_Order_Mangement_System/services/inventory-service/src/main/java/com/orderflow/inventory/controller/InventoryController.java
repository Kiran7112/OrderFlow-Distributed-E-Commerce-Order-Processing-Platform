package com.orderflow.inventory.controller;

import com.orderflow.inventory.dto.StockDto;
import com.orderflow.inventory.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*", maxAge = 3600)
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @GetMapping("/{productId}")
    public ResponseEntity<StockDto> getStock(@PathVariable UUID productId) {
        log.debug("GET /api/inventory/{} - Fetching stock", productId);
        try {
            StockDto stock = inventoryService.getStockByProductId(productId);
            return ResponseEntity.ok(stock);
        } catch (RuntimeException e) {
            log.warn("Stock not found for product: {}", productId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/{productId}")
    public ResponseEntity<StockDto> updateStock(
            @PathVariable UUID productId,
            @RequestParam Integer availableQty,
            @RequestParam(defaultValue = "10") Integer reorderLevel) {
        log.info("PUT /api/inventory/{} - Updating stock", productId);
        try {
            inventoryService.updateStock(productId, availableQty, reorderLevel);
            StockDto updated = inventoryService.getStockByProductId(productId);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            log.warn("Error updating stock for product {}: {}", productId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<StockDto>> getLowStockItems() {
        log.debug("GET /api/inventory/low-stock - Fetching low stock items");
        List<StockDto> items = inventoryService.getLowStockItems();
        return ResponseEntity.ok(items);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Inventory Service is healthy");
    }
}
