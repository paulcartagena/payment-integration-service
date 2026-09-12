package com.paulcartagena.paymentintegration.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;

@Entity
@Table(name = "PAYMENTS")
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    @Id
    private String id;

    @Column(name = "CUSTOMER_ID", nullable = false)
    private String customerId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "PAYMENT_TIMESTAMP", nullable = false)
    private OffsetDateTime timestamp;

    @Column(name = "RECEIVED_AT", nullable = false)
    private Instant receivedAt;

    @Column(name = "CORE_XML_PATH")
    private String coreXmlPath;
}
