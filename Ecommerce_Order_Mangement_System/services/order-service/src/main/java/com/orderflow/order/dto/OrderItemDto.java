package com.orderflow.order.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID productId;
    private Integer quantity;
    private BigDecimal unitPrice;

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(new BigDecimal(quantity));
    }
}
