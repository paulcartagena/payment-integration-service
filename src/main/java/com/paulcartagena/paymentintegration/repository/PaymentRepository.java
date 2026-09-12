package com.paulcartagena.paymentintegration.repository;

import com.paulcartagena.paymentintegration.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, String> {
}
