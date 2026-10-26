package com.daniele.apigateway.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class GatewayJwtTokenService {

    private static final String HMAC_ALGO = "HmacSHA256";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final JsonMapper jsonMapper;
    private final byte[] signingKey;
    private final long tokenTtlHours;

    public GatewayJwtTokenService(
            JsonMapper jsonMapper,
            @Value("${gateway.security.jwt-secret:expense-tracker-gateway-jwt-signing-secret-key-2026}") String jwtSecret,
            @Value("${gateway.security.token-ttl-hours:720}") long tokenTtlHours
    ) {
        this.jsonMapper = jsonMapper;
        this.signingKey = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.tokenTtlHours = tokenTtlHours;
    }

    public record VerifiedPrincipal(String subjectEmail, String authMethod, Instant expiresAt) {
    }

    public String issueToken(String subjectEmail, String authMethod) {
        Instant now = Instant.now();
        Instant exp = now.plus(tokenTtlHours, ChronoUnit.HOURS);

        String headerJson = jsonMapper.writeValueAsString(Map.of(
                "alg", "HS256",
                "typ", "JWT"
        ));
        String payloadJson = jsonMapper.writeValueAsString(Map.of(
                "sub", subjectEmail,
                "amr", authMethod,
                "iat", now.getEpochSecond(),
                "exp", exp.getEpochSecond()
        ));

        String encodedHeader = URL_ENCODER.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = URL_ENCODER.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = encodedHeader + "." + encodedPayload;
        String signature = signHmac(signingInput);

        return signingInput + "." + signature;
    }

    public Optional<VerifiedPrincipal> verifyToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String[] parts = token.trim().split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }
        try {
            String signingInput = parts[0] + "." + parts[1];
            String expectedSignature = signHmac(signingInput);
            if (!MessageDigest.isEqual(
                    parts[2].getBytes(StandardCharsets.UTF_8),
                    expectedSignature.getBytes(StandardCharsets.UTF_8)
            )) {
                return Optional.empty();
            }

            byte[] payloadBytes = URL_DECODER.decode(parts[1]);
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = jsonMapper.readValue(payloadBytes, Map.class);

            Object expRaw = claims.get("exp");
            if (!(expRaw instanceof Number expNumber)) {
                return Optional.empty();
            }
            Instant expiresAt = Instant.ofEpochSecond(expNumber.longValue());
            if (Instant.now().isAfter(expiresAt)) {
                return Optional.empty();
            }

            String sub = String.valueOf(claims.getOrDefault("sub", ""));
            String amr = String.valueOf(claims.getOrDefault("amr", "JWT"));
            if (sub.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new VerifiedPrincipal(sub, amr, expiresAt));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public long getTokenTtlHours() {
        return tokenTtlHours;
    }

    private String signHmac(String input) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(signingKey, HMAC_ALGO));
            byte[] rawHmac = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return URL_ENCODER.encodeToString(rawHmac);
        } catch (Exception e) {
            throw new IllegalStateException("Errore durante la firma HMAC-SHA256 del token JWT", e);
        }
    }
}
