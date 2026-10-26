package com.daniele.apigateway.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestGoogleOidcTokenVerifier implements GoogleOidcTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(RestGoogleOidcTokenVerifier.class);

    private final RestClient restClient;
    private final String expectedClientId;

    public RestGoogleOidcTokenVerifier(
            @Value("${gateway.security.google-client-id:}") String expectedClientId
    ) {
        this.restClient = RestClient.builder().build();
        this.expectedClientId = expectedClientId != null ? expectedClientId.trim() : "";
    }

    @Override
    public Optional<GoogleUserIdentity> verifyIdToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            return Optional.empty();
        }
        try {
            GoogleTokenInfoResponse info = restClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?id_token={token}", idToken.trim())
                    .retrieve()
                    .body(GoogleTokenInfoResponse.class);

            if (info == null || info.email() == null || info.email().isBlank()) {
                return Optional.empty();
            }
            boolean verified = "true".equalsIgnoreCase(info.emailVerified());
            if (!verified) {
                return Optional.empty();
            }
            if (!expectedClientId.isBlank() && !expectedClientId.equals(info.aud())) {
                log.warn("Google ID token audience non corrispondente: atteso={}, ricevuto={}", expectedClientId, info.aud());
                return Optional.empty();
            }
            return Optional.of(new GoogleUserIdentity(info.email().trim(), info.name(), true));
        } catch (Exception ex) {
            log.debug("Verifica Google ID token non riuscita: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleTokenInfoResponse(
            @JsonProperty("email") String email,
            @JsonProperty("email_verified") String emailVerified,
            @JsonProperty("name") String name,
            @JsonProperty("aud") String aud
    ) {
    }
}
