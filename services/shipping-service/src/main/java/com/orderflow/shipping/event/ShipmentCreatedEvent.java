package com.orderflow.shipping.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentCreatedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("order_id")
    private UUID orderId;

    @JsonProperty("shipment_id")
    private String shipmentId;

    @JsonProperty("tracking_number")
    private String trackingNumber;

    @JsonProperty("carrier")
    private String carrier;

    @JsonProperty("estimated_delivery")
    private LocalDate estimatedDelivery;

    @JsonProperty("status")
    private String status = "shipment.created";

    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private Long timestamp = System.currentTimeMillis();
}
