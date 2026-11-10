package com.franchise.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI franchiseOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Franchise Management Reactive API")
                        .version("1.0.0")
                        .description("Non-blocking REST API for managing franchises, branches, and inventory stock.")
                        .contact(new Contact().name("Backend Engineering")));
    }
}
