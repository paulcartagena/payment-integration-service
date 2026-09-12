package com.paulcartagena.paymentintegration.controller;

import com.paulcartagena.paymentintegration.dto.PaymentRequest;
import com.paulcartagena.paymentintegration.dto.PaymentResponse;
import com.paulcartagena.paymentintegration.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody PaymentRequest request,
                                                  UriComponentsBuilder uriBuilder) {
        var payment = paymentService.process(request);
        URI location = uriBuilder.path("/payments/{id}").buildAndExpand(payment.getId()).toUri();
        return ResponseEntity.created(location).body(PaymentResponse.from(payment));
    }
}
