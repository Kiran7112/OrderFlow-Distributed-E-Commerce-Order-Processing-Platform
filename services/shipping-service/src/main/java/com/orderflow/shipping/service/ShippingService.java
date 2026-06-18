package com.orderflow.shipping.service;

import com.orderflow.shipping.dto.ShipmentDto;
import com.orderflow.shipping.entity.Shipment;
import com.orderflow.shipping.kafka.ShippingEventPublisher;
import com.orderflow.shipping.repository.ShipmentRepository;
import com.orderflow.shipping.simulator.ShipmentDeliverySimulator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
public class ShippingService {

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private ShippingEventPublisher eventPublisher;

    @Autowired
    private ShipmentDeliverySimulator deliverySimulator;

    private static final Random random = new Random();

    @Transactional
    public ShipmentDto createShipment(UUID orderId) {
        log.info("Creating shipment for order: {}", orderId);

        Shipment existing = shipmentRepository.findByOrderId(orderId).orElse(null);
        if (existing != null) {
            log.warn("Shipment already exists for order: {}", orderId);
            return convertToDto(existing);
        }

        String trackingNumber = generateTrackingNumber();
        LocalDate estimatedDelivery = LocalDate.now().plusDays(5);

        Shipment shipment = Shipment.builder()
                .orderId(orderId)
                .trackingNumber(trackingNumber)
                .carrier("Express Courier")
                .status(Shipment.ShipmentStatus.CREATED)
                .estimatedDelivery(estimatedDelivery)
                .build();

        Shipment saved = shipmentRepository.save(shipment);
        log.info("Shipment created for order {}: tracking={}", orderId, trackingNumber);

        eventPublisher.publishShipmentCreated(
                orderId,
                saved.getId().toString(),
                trackingNumber,
                "Express Courier",
                estimatedDelivery
        );

        deliverySimulator.simulateDelivery(saved.getId());

        return convertToDto(saved);
    }

    public ShipmentDto getShipmentByOrderId(UUID orderId) {
        log.debug("Fetching shipment for order: {}", orderId);
        Shipment shipment = shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Shipment not found for order: " + orderId));
        return convertToDto(shipment);
    }

    public ShipmentDto getShipmentByTrackingNumber(String trackingNumber) {
        log.debug("Fetching shipment with tracking number: {}", trackingNumber);
        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new RuntimeException("Shipment not found: " + trackingNumber));
        return convertToDto(shipment);
    }

    private String generateTrackingNumber() {
        int year = LocalDate.now().getYear();
        int randomNum = 10000 + random.nextInt(90000);
        return String.format("TRK-%d-%d", year, randomNum);
    }

    private ShipmentDto convertToDto(Shipment shipment) {
        return ShipmentDto.builder()
                .id(shipment.getId())
                .orderId(shipment.getOrderId())
                .trackingNumber(shipment.getTrackingNumber())
                .carrier(shipment.getCarrier())
                .status(shipment.getStatus().name())
                .estimatedDelivery(shipment.getEstimatedDelivery())
                .actualDeliveryDate(shipment.getActualDeliveryDate())
                .createdAt(shipment.getCreatedAt())
                .updatedAt(shipment.getUpdatedAt())
                .build();
    }
}
