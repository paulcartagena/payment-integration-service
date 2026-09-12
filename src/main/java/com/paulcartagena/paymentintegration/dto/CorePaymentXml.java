package com.paulcartagena.paymentintegration.dto;

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import tools.jackson.dataformat.xml.annotation.JacksonXmlText;

import java.math.BigDecimal;

@JacksonXmlRootElement(localName = "PaymentMessage")
public record CorePaymentXml(
        @JacksonXmlProperty(localName = "Header") Header header,
        @JacksonXmlProperty(localName = "Body") Body body
        ) {
    public record Header(
            @JacksonXmlProperty(localName = "MessageId") String messageId,
            @JacksonXmlProperty(localName = "Channel") String channel,
            @JacksonXmlProperty(localName = "GeneratedAt") String generatedAt
    ) {}

    public record Body(
            @JacksonXmlProperty(localName = "TransactionId") String transactionId,
            @JacksonXmlProperty(localName = "CustomerRef") String customerRef,
            @JacksonXmlProperty(localName = "Amount") Amount amount,
            @JacksonXmlProperty(localName = "ValueDate") String valueDate
    ) {}

    public record Amount(
            @JacksonXmlProperty(localName = "currency", isAttribute = true) String currency,
            @JacksonXmlText BigDecimal value
    ) {}
}
