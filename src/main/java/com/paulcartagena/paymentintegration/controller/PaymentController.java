package com.paulcartagena.paymentintegration.controller;

import com.paulcartagena.paymentintegration.dto.PaymentRequest;
import com.paulcartagena.paymentintegration.dto.PaymentResponse;
import com.paulcartagena.paymentintegration.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

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

    @GetMapping
    public List<PaymentResponse> list(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {

        return paymentService.search(customerId, from, to)
                .stream()
                .map(PaymentResponse::from)
                .toList();
    }
}

