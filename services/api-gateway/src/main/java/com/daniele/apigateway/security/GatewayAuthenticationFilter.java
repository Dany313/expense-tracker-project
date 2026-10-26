package com.daniele.apigateway.security;

import com.daniele.apigateway.security.GatewayJwtTokenService.VerifiedPrincipal;
import com.daniele.apigateway.security.GoogleOidcTokenVerifier.GoogleUserIdentity;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTHENTICATED_EMAIL_ATTR = "gateway.authenticated.email";

    private final boolean authEnabled;
    private final String allowedEmail;
    private final GatewayJwtTokenService jwtTokenService;
    private final GoogleOidcTokenVerifier googleOidcTokenVerifier;

    public GatewayAuthenticationFilter(
            @Value("${gateway.security.auth-enabled:true}") boolean authEnabled,
            @Value("${gateway.security.allowed-email:daniele@example.com}") String allowedEmail,
            GatewayJwtTokenService jwtTokenService,
            GoogleOidcTokenVerifier googleOidcTokenVerifier
    ) {
        this.authEnabled = authEnabled;
        this.allowedEmail = allowedEmail != null ? allowedEmail.trim().toLowerCase() : "";
        this.jwtTokenService = jwtTokenService;
        this.googleOidcTokenVerifier = googleOidcTokenVerifier;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        // Endpoint pubblici sul Gateway: autenticazione, webhook Telegram (protetto da Secret Token + User ID Whitelist) e healthcheck
        return path.startsWith("/api/v1/auth")
                || path.startsWith("/api/v1/telegram/webhook")
                || path.startsWith("/actuator/health")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/api-docs");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!authEnabled) {
            request.setAttribute(AUTHENTICATED_EMAIL_ATTR, allowedEmail);
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeProblemDetail(
                    response,
                    HttpStatus.UNAUTHORIZED,
                    "Autenticazione richiesta",
                    "Effettua il login sul Gateway (OIDC Google o Passphrase) e invia l'header Authorization: Bearer <token>",
                    request.getRequestURI()
            );
            return;
        }

        String rawToken = authHeader.substring("Bearer ".length()).trim();

        // 1. Verifica token JWT emesso dal Gateway
        Optional<VerifiedPrincipal> gatewayJwt = jwtTokenService.verifyToken(rawToken);
        if (gatewayJwt.isPresent()) {
            String email = gatewayJwt.get().subjectEmail().toLowerCase();
            if (!isEmailAllowed(email)) {
                writeProblemDetail(
                        response,
                        HttpStatus.FORBIDDEN,
                        "Utente non autorizzato",
                        "L'account " + email + " non è autorizzato ad accedere a questo Expense Tracker",
                        request.getRequestURI()
                );
                return;
            }
            request.setAttribute(AUTHENTICATED_EMAIL_ATTR, email);
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Fallback: verifica diretta di un ID Token OIDC di Google
        Optional<GoogleUserIdentity> googleIdentity = googleOidcTokenVerifier.verifyIdToken(rawToken);
        if (googleIdentity.isPresent()) {
            String email = googleIdentity.get().email().toLowerCase();
            if (!isEmailAllowed(email)) {
                writeProblemDetail(
                        response,
                        HttpStatus.FORBIDDEN,
                        "Account Google non in whitelist",
                        "L'account Google " + email + " non corrisponde all'utente autorizzato",
                        request.getRequestURI()
                );
                return;
            }
            request.setAttribute(AUTHENTICATED_EMAIL_ATTR, email);
            filterChain.doFilter(request, response);
            return;
        }

        writeProblemDetail(
                response,
                HttpStatus.UNAUTHORIZED,
                "Token non valido o scaduto",
                "Il token Bearer fornito non è valido o è scaduto. Effettua nuovamente l'accesso.",
                request.getRequestURI()
        );
    }

    public boolean isEmailAllowed(String email) {
        if (allowedEmail.isBlank()) {
            return true;
        }
        return allowedEmail.equalsIgnoreCase(email.trim());
    }

    private void writeProblemDetail(
            HttpServletResponse response,
            HttpStatus status,
            String title,
            String detail,
            String instance
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {
                  "type": "about:blank",
                  "title": "%s",
                  "status": %d,
                  "detail": "%s",
                  "instance": "%s",
                  "timestamp": "%s"
                }
                """.formatted(title, status.value(), detail, instance, Instant.now()));
    }
}
