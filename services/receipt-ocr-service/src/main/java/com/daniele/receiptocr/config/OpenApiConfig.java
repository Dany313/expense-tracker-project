package com.daniele.receiptocr.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI receiptOcrOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Receipt OCR Microservice API")
                        .version("1.0.0")
                        .description("Microservizio AI di OCR scontrini con Spring AI Vision e Server MCP.")
                        .contact(new Contact().name("Daniele")));
    }
}
