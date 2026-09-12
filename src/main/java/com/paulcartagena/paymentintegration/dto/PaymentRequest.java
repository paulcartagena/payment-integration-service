package com.paulcartagena.paymentintegration.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentRequest(

        @NotBlank(message = "id es obligatorio")
        String id,

        @NotBlank(message = "customerId es obligatorio")
        String customerId,

        @NotNull(message = "amount es obligatorio")
        @DecimalMin(value = "0.00", inclusive = false, message = "amount debe ser mayor a cero")
        @Digits(integer = 17, fraction = 2, message = "amount admite hasta 2 decimales")
        BigDecimal amount,

        @NotBlank(message = "currency es obligatorio")
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency debe ser un código ISO-4217 de 3 letras")
        String currency,

        @NotNull(message = "timestamp es obligatorio")
        OffsetDateTime timestamp
) {
}
