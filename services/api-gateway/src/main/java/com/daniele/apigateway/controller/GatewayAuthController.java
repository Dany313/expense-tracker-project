package com.daniele.apigateway.controller;

import com.daniele.apigateway.security.GatewayAuthenticationFilter;
import com.daniele.apigateway.security.GatewayJwtTokenService;
import com.daniele.apigateway.security.GatewayJwtTokenService.VerifiedPrincipal;
import com.daniele.apigateway.security.GoogleOidcTokenVerifier;
import com.daniele.apigateway.security.GoogleOidcTokenVerifier.GoogleUserIdentity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Gateway Authentication (OIDC & Access Token)", description = "Autenticazione utente sul Gateway (Google OIDC con whitelist email oppure Passphrase personale)")
public class GatewayAuthController {

    private final boolean authEnabled;
    private final String allowedEmail;
    private final String googleClientId;
    private final String accessPassphrase;
    private final GatewayJwtTokenService jwtTokenService;
    private final GoogleOidcTokenVerifier googleOidcTokenVerifier;
    private final GatewayAuthenticationFilter authFilter;

    public GatewayAuthController(
            @Value("${gateway.security.auth-enabled:true}") boolean authEnabled,
            @Value("${gateway.security.allowed-email:daniele@example.com}") String allowedEmail,
            @Value("${gateway.security.google-client-id:}") String googleClientId,
            @Value("${gateway.security.access-passphrase:admin123}") String accessPassphrase,
            GatewayJwtTokenService jwtTokenService,
            GoogleOidcTokenVerifier googleOidcTokenVerifier,
            GatewayAuthenticationFilter authFilter
    ) {
        this.authEnabled = authEnabled;
        this.allowedEmail = allowedEmail != null ? allowedEmail.trim() : "";
        this.googleClientId = googleClientId != null ? googleClientId.trim() : "";
        this.accessPassphrase = accessPassphrase != null ? accessPassphrase.trim() : "";
        this.jwtTokenService = jwtTokenService;
        this.googleOidcTokenVerifier = googleOidcTokenVerifier;
        this.authFilter = authFilter;
    }

    public record PassphraseLoginRequest(@NotBlank(message = "La passphrase è obbligatoria") String passphrase) {
    }

    public record GoogleOidcLoginRequest(@NotBlank(message = "Il Google idToken è obbligatorio") String idToken) {
    }

    public record AuthTokenResponse(
            String accessToken,
            String tokenType,
            String userEmail,
            String authMethod,
            long expiresInHours,
            Instant issuedAt
    ) {
    }

    public record AuthStatusResponse(
            boolean authRequired,
            boolean authenticated,
            String userEmail,
            boolean googleOidcConfigured,
            String googleClientId
    ) {
    }

    @GetMapping("/status")
    @Operation(summary = "Verifica lo stato di autenticazione corrente sul Gateway")
    public ResponseEntity<AuthStatusResponse> getAuthStatus(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        if (!authEnabled) {
            return ResponseEntity.ok(new AuthStatusResponse(
                    false,
                    true,
                    allowedEmail,
                    !googleClientId.isBlank(),
                    googleClientId
            ));
        }

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring("Bearer ".length()).trim();
            Optional<VerifiedPrincipal> verified = jwtTokenService.verifyToken(token);
            if (verified.isPresent() && authFilter.isEmailAllowed(verified.get().subjectEmail())) {
                return ResponseEntity.ok(new AuthStatusResponse(
                        true,
                        true,
                        verified.get().subjectEmail(),
                        !googleClientId.isBlank(),
                        googleClientId
                ));
            }
        }

        return ResponseEntity.ok(new AuthStatusResponse(
                true,
                false,
                null,
                !googleClientId.isBlank(),
                googleClientId
        ));
    }

    @PostMapping("/login")
    @Operation(summary = "Esegue il login con la passphrase personale configurata sul Gateway e restituisce un JWT firmato")
    public ResponseEntity<?> loginWithPassphrase(@Valid @RequestBody PassphraseLoginRequest request) {
        if (accessPassphrase.isBlank() || !MessageDigest.isEqual(
                request.passphrase().trim().getBytes(StandardCharsets.UTF_8),
                accessPassphrase.getBytes(StandardCharsets.UTF_8)
        )) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.UNAUTHORIZED,
                    "Passphrase di accesso non valida"
            );
            problem.setTitle("Credenziali non valide");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
        }

        String subject = allowedEmail.isBlank() ? "owner@expense-tracker.local" : allowedEmail;
        String jwt = jwtTokenService.issueToken(subject, "PASSPHRASE");
        return ResponseEntity.ok(new AuthTokenResponse(
                jwt,
                "Bearer",
                subject,
                "PASSPHRASE",
                jwtTokenService.getTokenTtlHours(),
                Instant.now()
        ));
    }

    @PostMapping("/google")
    @Operation(summary = "Esegue il login tramite Google OIDC ID Token verificando la whitelist sulla tua email")
    public ResponseEntity<?> loginWithGoogleOidc(@Valid @RequestBody GoogleOidcLoginRequest request) {
        Optional<GoogleUserIdentity> identityOpt = googleOidcTokenVerifier.verifyIdToken(request.idToken());
        if (identityOpt.isEmpty()) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.UNAUTHORIZED,
                    "Il Google ID Token fornito non è valido o è scaduto"
            );
            problem.setTitle("Google OIDC Token non valido");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
        }

        GoogleUserIdentity identity = identityOpt.get();
        if (!authFilter.isEmailAllowed(identity.email())) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.FORBIDDEN,
                    "L'account Google " + identity.email() + " non è autorizzato ad accedere a questo Expense Tracker personale"
            );
            problem.setTitle("Account Google non in whitelist");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
        }

        String jwt = jwtTokenService.issueToken(identity.email(), "GOOGLE_OIDC");
        return ResponseEntity.ok(new AuthTokenResponse(
                jwt,
                "Bearer",
                identity.email(),
                "GOOGLE_OIDC",
                jwtTokenService.getTokenTtlHours(),
                Instant.now()
        ));
    }
}
