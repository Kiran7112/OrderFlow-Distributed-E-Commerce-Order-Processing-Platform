package com.orderflow.notification.event;

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
public class ShippingStatusEvent implements Serializable {

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

    @JsonProperty("status")
    private String status;

    @JsonProperty("estimated_delivery")
    private LocalDate estimatedDelivery;

    @JsonProperty("actual_delivery_date")
    private LocalDate actualDeliveryDate;

    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private Long timestamp;
}
