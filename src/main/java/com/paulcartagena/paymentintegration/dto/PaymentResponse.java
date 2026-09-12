package com.paulcartagena.paymentintegration.dto;

import com.paulcartagena.paymentintegration.domain.Payment;
import com.paulcartagena.paymentintegration.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;

public record PaymentResponse(
        String id,
        String customerId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        OffsetDateTime timestamp,
        Instant receivedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getCustomerId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getTimestamp(),
                payment.getReceivedAt()
        );
    }
}
