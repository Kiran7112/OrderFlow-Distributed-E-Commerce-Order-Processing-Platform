package com.orderflow.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock", indexes = {
        @Index(name = "idx_product_id", columnList = "product_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(nullable = false)
    private Integer availableQty;

    @Column(nullable = false)
    private Integer reservedQty = 0;

    @Column(nullable = false)
    private Integer reorderLevel = 10;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    @Column(length = 100)
    private String updatedBy;

    public int getTotalQty() {
        return availableQty + reservedQty;
    }

    public void reserve(int quantity) {
        if (availableQty < quantity) {
            throw new IllegalArgumentException(
                    "Insufficient stock: available=" + availableQty + ", requested=" + quantity);
        }
        availableQty -= quantity;
        reservedQty += quantity;
    }

    public void release(int quantity) {
        if (reservedQty < quantity) {
            throw new IllegalArgumentException(
                    "Cannot release more than reserved: reserved=" + reservedQty + ", requested=" + quantity);
        }
        reservedQty -= quantity;
        availableQty += quantity;
    }

    public void confirm(int quantity) {
        if (reservedQty < quantity) {
            throw new IllegalArgumentException(
                    "Cannot confirm more than reserved: reserved=" + reservedQty + ", requested=" + quantity);
        }
        reservedQty -= quantity;
    }

    public boolean isLowStock() {
        return getTotalQty() <= reorderLevel;
    }
}
