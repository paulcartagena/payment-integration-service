package com.paulcartagena.paymentintegration.repository;

import com.paulcartagena.paymentintegration.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    @Query("""
            SELECT p FROM Payment p
            WHERE(:customerId IS NULL OR p.customerId = :customerId)
            AND (:from IS NULL OR p.timestamp >= :from)
            AND (:to IS NULL OR p.timestamp <= :to)
            ORDER BY p.timestamp DESC
            """)
    List<Payment> search(@Param("customerId") String customerId,
                         @Param("from") OffsetDateTime from,
                         @Param("to") OffsetDateTime to);
}
