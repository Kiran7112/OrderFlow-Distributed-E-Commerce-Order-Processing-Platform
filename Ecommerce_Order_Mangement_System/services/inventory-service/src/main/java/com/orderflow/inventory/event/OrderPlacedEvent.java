package com.orderflow.inventory.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderPlacedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("order_id")
    private UUID orderId;

    @JsonProperty("customer_id")
    private UUID customerId;

    @JsonProperty("total_amount")
    private BigDecimal totalAmount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("items")
    private List<OrderItemEvent> items;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemEvent implements Serializable {
        @JsonProperty("product_id")
        private UUID productId;

        @JsonProperty("quantity")
        private Integer quantity;

        @JsonProperty("unit_price")
        private BigDecimal unitPrice;
    }
}
