package com.daniele.receiptocr.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class InternalM2mSecurityFilter extends OncePerRequestFilter {

    public static final String M2M_HEADER_NAME = "X-Internal-M2M-Token";

    private final boolean m2mEnabled;
    private final String expectedToken;

    public InternalM2mSecurityFilter(
            @Value("${security.m2m.enabled:false}") boolean m2mEnabled,
            @Value("${security.m2m.secret:expense-tracker-internal-m2m-secret-change-me}") String expectedToken
    ) {
        this.m2mEnabled = m2mEnabled;
        this.expectedToken = expectedToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!m2mEnabled) {
            return true;
        }
        String path = request.getRequestURI();
        return path.startsWith("/actuator/health") || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String providedToken = request.getHeader(M2M_HEADER_NAME);
        if (!isValidToken(providedToken)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("""
                    {
                      "type": "about:blank",
                      "title": "Accesso M2M non autorizzato",
                      "status": 401,
                      "detail": "Token Machine-to-Machine (X-Internal-M2M-Token) mancante o non valido",
                      "instance": "%s",
                      "timestamp": "%s"
                    }
                    """.formatted(request.getRequestURI(), Instant.now()));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isValidToken(String providedToken) {
        if (providedToken == null || providedToken.isBlank() || expectedToken == null || expectedToken.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                providedToken.trim().getBytes(StandardCharsets.UTF_8),
                expectedToken.trim().getBytes(StandardCharsets.UTF_8)
        );
    }
}
