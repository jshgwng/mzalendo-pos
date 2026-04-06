package com.joshuaogwang.mzalendopos.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mzalendo POS API")
                        .description("REST API for Mzalendo Point-of-Sale system. " +
                                "Authenticate via POST /api/v1/auth/login to obtain a JWT token, " +
                                "then use the Authorize button to set it as a Bearer token.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Joshua Ogwang")
                                .email("joshuaogwang@example.com")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter the JWT token obtained from /api/v1/auth/login")));
    }
}
