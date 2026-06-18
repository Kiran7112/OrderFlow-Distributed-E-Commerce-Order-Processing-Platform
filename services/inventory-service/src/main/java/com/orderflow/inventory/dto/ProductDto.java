package com.orderflow.inventory.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private String sku;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
