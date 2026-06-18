package com.orderflow.shipping.controller;

import com.orderflow.shipping.dto.ShipmentDto;
import com.orderflow.shipping.service.ShippingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/shipping")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ShippingController {

    @Autowired
    private ShippingService shippingService;

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ShipmentDto> getShipmentByOrderId(@PathVariable UUID orderId) {
        log.debug("GET /api/shipping/order/{} - Fetching shipment", orderId);
        try {
            ShipmentDto shipment = shippingService.getShipmentByOrderId(orderId);
            return ResponseEntity.ok(shipment);
        } catch (RuntimeException e) {
            log.warn("Shipment not found for order: {}", orderId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/tracking/{trackingNumber}")
    public ResponseEntity<ShipmentDto> getShipmentByTrackingNumber(@PathVariable String trackingNumber) {
        log.debug("GET /api/shipping/tracking/{} - Fetching shipment", trackingNumber);
        try {
            ShipmentDto shipment = shippingService.getShipmentByTrackingNumber(trackingNumber);
            return ResponseEntity.ok(shipment);
        } catch (RuntimeException e) {
            log.warn("Shipment not found: {}", trackingNumber);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Shipping Service is healthy");
    }
}
