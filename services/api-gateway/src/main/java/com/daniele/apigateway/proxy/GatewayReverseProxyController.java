package com.daniele.apigateway.proxy;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Hidden
public class GatewayReverseProxyController {

    private final BackendProxyService backendProxyService;

    public GatewayReverseProxyController(BackendProxyService backendProxyService) {
        this.backendProxyService = backendProxyService;
    }

    @RequestMapping({"/api/v1/expenses", "/api/v1/expenses/**"})
    public ResponseEntity<?> proxyExpenses(
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body
    ) {
        return backendProxyService.forwardToExpenseService(request, body);
    }

    @RequestMapping({"/api/v1/ocr", "/api/v1/ocr/**"})
    public ResponseEntity<?> proxyReceiptOcr(
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body
    ) {
        return backendProxyService.forwardToReceiptOcrService(request, body);
    }

    @RequestMapping({"/api/v1/agent", "/api/v1/agent/**", "/api/v1/telegram", "/api/v1/telegram/**"})
    public ResponseEntity<?> proxyExpenseAgentAndTelegram(
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body
    ) {
        return backendProxyService.forwardToExpenseAgentService(request, body);
    }
}
