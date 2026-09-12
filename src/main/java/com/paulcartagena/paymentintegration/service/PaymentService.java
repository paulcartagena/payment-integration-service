package com.paulcartagena.paymentintegration.service;

import com.paulcartagena.paymentintegration.domain.Payment;
import com.paulcartagena.paymentintegration.domain.PaymentStatus;
import com.paulcartagena.paymentintegration.dto.PaymentRequest;
import com.paulcartagena.paymentintegration.exception.DuplicatePaymentException;
import com.paulcartagena.paymentintegration.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final CoreXmlSender coreXmlSender;

    public PaymentService(PaymentRepository repository, PaymentMapper mapper, CoreXmlSender coreXmlSender) {
        this.paymentRepository = repository;
        this.paymentMapper = mapper;
        this.coreXmlSender = coreXmlSender;
    }

    @Transactional
    public Payment process(PaymentRequest request) {
        if (paymentRepository.existsById(request.id())) {
            throw new DuplicatePaymentException(request.id());
        }

        Payment payment = paymentMapper.toEntity(request);
        String xmlPath = coreXmlSender.send(paymentMapper.toCoreXml(payment));

        payment.setCoreXmlPath(xmlPath);
        payment.setStatus(PaymentStatus.SENT);

        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public List<Payment> search(String customerId, OffsetDateTime from, OffsetDateTime to) {
        return paymentRepository.search(customerId, from, to);
    }
}
