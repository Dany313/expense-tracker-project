package com.daniele.receiptocr.config;

import com.daniele.receiptocr.mcp.ReceiptOcrMcpTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider receiptOcrToolCallbackProvider(ReceiptOcrMcpTools receiptOcrMcpTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(receiptOcrMcpTools)
                .build();
    }
}
