package com.orderflow.inventory.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID productId;
    private Integer availableQty;
    private Integer reservedQty;
    private Integer totalQty;
    private Integer reorderLevel;
    private Boolean isLowStock;
    private LocalDateTime lastUpdated;
}
