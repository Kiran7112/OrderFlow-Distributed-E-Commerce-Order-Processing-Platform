package com.orderflow.payment.repository;

import com.orderflow.payment.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> findByOrderId(UUID orderId);
    List<Transaction> findByStatus(Transaction.PaymentStatus status);
}
