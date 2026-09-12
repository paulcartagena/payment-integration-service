package com.paulcartagena.paymentintegration.exception;

public class DuplicatePaymentException extends RuntimeException {

    public DuplicatePaymentException(String id) {
        super("Ya existe un pago con id " + id);
    }
}
