package com.joshuaogwang.mzalendopos.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import com.joshuaogwang.mzalendopos.config.EfrisProperties;

import java.time.Duration;

@Configuration
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, EfrisProperties efrisProperties) {
        return builder
                .connectTimeout(Duration.ofMillis(efrisProperties.getTimeoutMs()))
                .readTimeout(Duration.ofMillis(efrisProperties.getTimeoutMs()))
                .build();
    }
}
