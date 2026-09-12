package com.paulcartagena.paymentintegration.service;

import com.paulcartagena.paymentintegration.domain.Payment;
import com.paulcartagena.paymentintegration.domain.PaymentStatus;
import com.paulcartagena.paymentintegration.dto.CorePaymentXml;
import com.paulcartagena.paymentintegration.dto.PaymentRequest;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Traduce el pago entre los tres formatos del flujo: el JSON del canal digital,
 * la entidad persistida y el mensaje XML que espera el sistema core.
 * **/
@Component
public class PaymentMapper {

    private static final String CHANNEL = "DIGITAL";
    private static final DateTimeFormatter ISO_UTC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter VALUE_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    private final Clock clock;

    public PaymentMapper(Clock clock) {
        this.clock = clock;
    }

    public Payment toEntity(PaymentRequest request) {
        Payment payment = new Payment();
        payment.setId(request.id());
        payment.setCustomerId(request.customerId());
        payment.setAmount(request.amount());
        payment.setCurrency(request.currency());
        payment.setTimestamp(request.timestamp());
        payment.setReceivedAt(clock.instant());
        payment.setStatus(PaymentStatus.RECEIVED);
        return payment;
    }

    public CorePaymentXml toCoreXml(Payment payment) {
        var header = new CorePaymentXml.Header(
                payment.getId(),
                CHANNEL,
                ISO_UTC.format(clock.instant())
        );

        var amount = new CorePaymentXml.Amount(
                payment.getCurrency(),
                payment.getAmount()
        );

        var body = new CorePaymentXml.Body(
                payment.getId(),
                payment.getCustomerId(),
                amount,
                VALUE_DATE.format(payment.getTimestamp())
        );

        return new CorePaymentXml(header, body);
    }
}
