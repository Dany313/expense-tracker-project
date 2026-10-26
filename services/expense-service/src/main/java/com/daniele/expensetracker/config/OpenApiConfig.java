package com.daniele.expensetracker.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI expenseTrackerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Expense Tracker Microservice API")
                        .version("1.0.0")
                        .description("API REST e Server MCP per la gestione delle spese su PostgreSQL.")
                        .contact(new Contact().name("Daniele")));
    }
}
