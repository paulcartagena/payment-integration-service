package com.paulcartagena.paymentintegration.service;

import com.paulcartagena.paymentintegration.dto.CorePaymentXml;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.dataformat.xml.XmlMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simula el envío al sistema persistiendo el mensaje XML en disco.
 * **/
@Component
public class CoreXmlSender {

    private final XmlMapper xmlMapper;
    private final Path outboxDir;

    public CoreXmlSender(@Value("${core.outbox-dir:core-outbox}") String outboxDir) {
        this.xmlMapper = new XmlMapper();
        this.outboxDir = Path.of(outboxDir);
    }

    public String send(CorePaymentXml message) {
        try {
            Files.createDirectories(outboxDir);
            Path file = outboxDir.resolve(message.body().transactionId() + ".xml");
            Files.writeString(file, xmlMapper.writeValueAsString(message));
            return file.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo escribir el XML para el core", e);
        }
    }
}
