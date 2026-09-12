package com.paulcartagena.paymentintegration.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Payment Integration Service",
                version = "1.0",
                description = "Recibe pagos en JSON desde el canal digital, los transforma al formato XML del sistema core y los envía."
        ),
        security = @SecurityRequirement(name = "basicAuth")
)
@SecurityScheme(
        name = "basicAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "basic"
)
public class OpenApiConfig {
}
