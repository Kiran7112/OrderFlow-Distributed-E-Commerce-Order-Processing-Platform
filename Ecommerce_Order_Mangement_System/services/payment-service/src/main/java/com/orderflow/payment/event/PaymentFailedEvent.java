package com.orderflow.payment.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentFailedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("order_id")
    private UUID orderId;

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("status")
    private String status = "payment.failed";

    @JsonProperty("failure_reason")
    private String failureReason;

    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private Long timestamp = System.currentTimeMillis();
}
