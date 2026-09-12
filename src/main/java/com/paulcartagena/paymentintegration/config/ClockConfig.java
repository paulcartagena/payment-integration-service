package com.paulcartagena.paymentintegration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    @Bean
    public Clock ClockConfig() {
        return Clock.systemUTC();
    }
}
