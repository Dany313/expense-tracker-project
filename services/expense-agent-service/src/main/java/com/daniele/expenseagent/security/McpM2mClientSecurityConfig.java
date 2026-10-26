package com.daniele.expenseagent.security;

import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import java.net.http.HttpRequest;
import org.springframework.ai.mcp.customizer.McpClientCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpM2mClientSecurityConfig {

    @Bean
    public McpClientCustomizer<HttpClientSseClientTransport.Builder> mcpM2mHeaderCustomizer(
            @Value("${security.m2m.secret:expense-tracker-internal-m2m-secret-change-me}") String m2mSecret
    ) {
        return (serverConfigurationName, builder) -> {
            if (m2mSecret != null && !m2mSecret.isBlank()) {
                builder.requestBuilder(
                        HttpRequest.newBuilder()
                                .header(InternalM2mSecurityFilter.M2M_HEADER_NAME, m2mSecret.trim())
                );
            }
        };
    }
}
