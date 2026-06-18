package com.orderflow.inventory.repository;

import com.orderflow.inventory.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockRepository extends JpaRepository<Stock, UUID> {
    Optional<Stock> findByProductId(UUID productId);

    @Query("SELECT s FROM Stock s WHERE (s.availableQty + s.reservedQty) <= s.reorderLevel")
    List<Stock> findLowStockItems();
}
