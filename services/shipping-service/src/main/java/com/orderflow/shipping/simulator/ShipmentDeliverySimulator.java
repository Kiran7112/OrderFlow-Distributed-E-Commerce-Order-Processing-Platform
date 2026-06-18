package com.orderflow.shipping.simulator;

import com.orderflow.shipping.entity.Shipment;
import com.orderflow.shipping.kafka.ShippingEventPublisher;
import com.orderflow.shipping.repository.ShipmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Component
public class ShipmentDeliverySimulator {

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private ShippingEventPublisher eventPublisher;

    @Value("${delivery.delay-seconds:30}")
    private long deliveryDelaySeconds;

    @Async
    public void simulateDelivery(UUID shipmentId) {
        try {
            log.info("Scheduling delivery simulation for shipment: {}, delay: {}s", shipmentId, deliveryDelaySeconds);

            Thread.sleep(deliveryDelaySeconds * 1000);

            Shipment shipment = shipmentRepository.findById(shipmentId)
                    .orElseThrow(() -> new RuntimeException("Shipment not found: " + shipmentId));

            shipment.setStatus(Shipment.ShipmentStatus.IN_TRANSIT);
            shipmentRepository.save(shipment);
            log.info("Shipment {} marked as IN_TRANSIT", shipmentId);

            Thread.sleep(1000);

            shipment.setStatus(Shipment.ShipmentStatus.DELIVERED);
            shipment.setActualDeliveryDate(LocalDate.now());
            Shipment delivered = shipmentRepository.save(shipment);
            log.info("Shipment {} marked as DELIVERED", shipmentId);

            eventPublisher.publishShipmentDelivered(
                    delivered.getOrderId(),
                    delivered.getId().toString(),
                    delivered.getTrackingNumber(),
                    delivered.getActualDeliveryDate()
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Delivery simulation interrupted for shipment: {}", shipmentId, e);
        } catch (Exception e) {
            log.error("Error simulating delivery for shipment: {}", shipmentId, e);
        }
    }
}
