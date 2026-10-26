package com.daniele.expenseagent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI expenseAgentOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Expense AI Agent Microservice API")
                        .version("1.0.0")
                        .description("Microservizio Agente AI (Testo e Vocale -> Gemini + MCP Client collegato a expense-service).")
                        .contact(new Contact().name("Daniele")));
    }
}
