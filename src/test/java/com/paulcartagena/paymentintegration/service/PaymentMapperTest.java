package com.paulcartagena.paymentintegration.service;

import com.paulcartagena.paymentintegration.domain.Payment;
import com.paulcartagena.paymentintegration.domain.PaymentStatus;
import com.paulcartagena.paymentintegration.dto.CorePaymentXml;
import com.paulcartagena.paymentintegration.dto.PaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.dataformat.xml.XmlMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class PaymentMapperTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-09-12T22:00:00Z");

    private PaymentMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PaymentMapper(Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    private PaymentRequest sampleRequest() {
        return new PaymentRequest(
                "PAY-001",
                "CUST-42",
                new BigDecimal("150.00"),
                "USD",
                OffsetDateTime.parse("2026-09-12T14:50:41-06:00")
        );
    }

    @Test
    @DisplayName("toEntity asigna el estado inicial y la hora de recepción del servidor")
    void toEntityAssignsServerControlledFields() {
        Payment payment = mapper.toEntity(sampleRequest());

        assertThat(payment.getId()).isEqualTo("PAY-001");
        assertThat(payment.getCustomerId()).isEqualTo("CUST-42");
        assertThat(payment.getAmount()).isEqualByComparingTo("150.00");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.RECEIVED);
        assertThat(payment.getReceivedAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    @DisplayName("toCoreXml renombra los campos al vocabulario del core")
    void toCoreXmlRenamesFields() {
        CorePaymentXml result = mapper.toCoreXml(mapper.toEntity(sampleRequest()));

        assertThat(result.header().messageId()).isEqualTo("PAY-001");
        assertThat(result.header().channel()).isEqualTo("DIGITAL");
        assertThat(result.body().transactionId()).isEqualTo("PAY-001");
        assertThat(result.body().customerRef()).isEqualTo("CUST-42");
        assertThat(result.body().amount().currency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("toCoreXml trunca el timestamp a fecha en UTC")
    void toCoreXmlTruncatesTimestampToUtcDate() {
        CorePaymentXml result = mapper.toCoreXml(mapper.toEntity(sampleRequest()));

        // 14:50-06:00 es 20:50Z del mismo día
        assertThat(result.body().valueDate()).isEqualTo("2026-09-12");
        assertThat(result.header().generatedAt()).isEqualTo("2026-09-12T22:00:00Z");
    }

    @Test
    @DisplayName("El XML serializado tiene la estructura que espera el core")
    void serializedXmlMatchesCoreStructure() {
        CorePaymentXml message = mapper.toCoreXml(mapper.toEntity(sampleRequest()));

        String xml = new XmlMapper().writeValueAsString(message);

        assertThat(xml)
                .contains("<MessageId>PAY-001</MessageId>")
                .contains("<CustomerRef>CUST-42</CustomerRef>")
                .contains("currency=\"USD\"")
                .contains("<ValueDate>2026-09-12</ValueDate>")
                .doesNotContain("customerId");
    }
}
