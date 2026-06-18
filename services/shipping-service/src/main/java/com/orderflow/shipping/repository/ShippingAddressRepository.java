package com.orderflow.shipping.repository;

import com.orderflow.shipping.entity.ShippingAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShippingAddressRepository extends JpaRepository<ShippingAddress, UUID> {
    Optional<ShippingAddress> findByOrderId(UUID orderId);
}
