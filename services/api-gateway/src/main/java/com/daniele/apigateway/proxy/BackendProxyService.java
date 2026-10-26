package com.daniele.apigateway.proxy;

import com.daniele.apigateway.security.GatewayAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.util.Enumeration;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class BackendProxyService {

    public static final String M2M_HEADER_NAME = "X-Internal-M2M-Token";
    public static final String AUTHENTICATED_USER_HEADER = "X-Authenticated-User";

    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "host",
            "connection",
            "content-length",
            "transfer-encoding",
            "keep-alive",
            "upgrade",
            "x-internal-m2m-token"
    );

    private final RestClient restClient;
    private final String expenseServiceUrl;
    private final String receiptOcrServiceUrl;
    private final String expenseAgentServiceUrl;
    private final String m2mSecret;

    public BackendProxyService(
            @Value("${gateway.routes.expense-service-url:http://localhost:8080}") String expenseServiceUrl,
            @Value("${gateway.routes.receipt-ocr-service-url:http://localhost:8081}") String receiptOcrServiceUrl,
            @Value("${gateway.routes.expense-agent-service-url:http://localhost:8082}") String expenseAgentServiceUrl,
            @Value("${gateway.security.m2m-secret:expense-tracker-internal-m2m-secret-change-me}") String m2mSecret
    ) {
        this.restClient = RestClient.builder()
                .defaultStatusHandler(status -> true, (req, res) -> {
                    // Non sollevare eccezioni sui 4xx/5xx dei microservizi: inoltra lo status code al client
                })
                .build();
        this.expenseServiceUrl = trimTrailingSlash(expenseServiceUrl);
        this.receiptOcrServiceUrl = trimTrailingSlash(receiptOcrServiceUrl);
        this.expenseAgentServiceUrl = trimTrailingSlash(expenseAgentServiceUrl);
        this.m2mSecret = m2mSecret != null ? m2mSecret.trim() : "";
    }

    public ResponseEntity<?> forwardToExpenseService(HttpServletRequest request, byte[] body) {
        return forwardRequest(expenseServiceUrl, request, body);
    }

    public ResponseEntity<?> forwardToReceiptOcrService(HttpServletRequest request, byte[] body) {
        return forwardRequest(receiptOcrServiceUrl, request, body);
    }

    public ResponseEntity<?> forwardToExpenseAgentService(HttpServletRequest request, byte[] body) {
        return forwardRequest(expenseAgentServiceUrl, request, body);
    }

    private ResponseEntity<?> forwardRequest(String targetBaseUrl, HttpServletRequest request, byte[] body) {
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String fullUrl = targetBaseUrl + path + (query != null && !query.isBlank() ? "?" + query : "");

        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        HttpHeaders outboundHeaders = buildOutboundHeaders(request);

        try {
            RestClient.RequestBodySpec spec = restClient.method(method)
                    .uri(URI.create(fullUrl))
                    .headers(h -> h.addAll(outboundHeaders));

            byte[] payload = body;
            if (payload == null && hasRequestBody(method)) {
                payload = request.getInputStream().readAllBytes();
            }
            if (payload != null && payload.length > 0 && hasRequestBody(method)) {
                spec.body(payload);
            }

            ResponseEntity<byte[]> downstreamResponse = spec.retrieve().toEntity(byte[].class);

            HttpHeaders responseHeaders = new HttpHeaders();
            MediaType contentType = downstreamResponse.getHeaders().getContentType();
            if (contentType != null) {
                responseHeaders.setContentType(contentType);
            }
            return new ResponseEntity<>(
                    downstreamResponse.getBody(),
                    responseHeaders,
                    downstreamResponse.getStatusCode()
            );
        } catch (IOException | RuntimeException ex) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.BAD_GATEWAY,
                    "Il microservizio di destinazione (" + targetBaseUrl + ") non è al momento raggiungibile: " + ex.getMessage()
            );
            problem.setTitle("Microservizio temporaneamente non disponibile");
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(problem);
        }
    }

    private HttpHeaders buildOutboundHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                if (!HOP_BY_HOP_HEADERS.contains(name.toLowerCase())) {
                    Enumeration<String> values = request.getHeaders(name);
                    while (values.hasMoreElements()) {
                        headers.add(name, values.nextElement());
                    }
                }
            }
        }
        // Iniezione automatica del token M2M interno verso i microservizi
        headers.set(M2M_HEADER_NAME, m2mSecret);

        Object authenticatedEmail = request.getAttribute(GatewayAuthenticationFilter.AUTHENTICATED_EMAIL_ATTR);
        if (authenticatedEmail != null) {
            headers.set(AUTHENTICATED_USER_HEADER, String.valueOf(authenticatedEmail));
        }
        return headers;
    }

    private static boolean hasRequestBody(HttpMethod method) {
        return method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH;
    }

    private static String trimTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
