package com.orderflow.order.dto;

import lombok.*;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID customerId;
    private List<OrderItemDto> items;
}
