package com.daniele.expensetracker.config;

import com.daniele.expensetracker.mcp.ExpenseMcpTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider expenseToolCallbackProvider(ExpenseMcpTools expenseMcpTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(expenseMcpTools)
                .build();
    }
}
