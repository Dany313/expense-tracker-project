package com.daniele.apigateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daniele.apigateway.proxy.BackendProxyService;
import com.daniele.apigateway.security.GatewayJwtTokenService;
import com.daniele.apigateway.security.GoogleOidcTokenVerifier;
import com.daniele.apigateway.security.GoogleOidcTokenVerifier.GoogleUserIdentity;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "gateway.security.auth-enabled=true",
        "gateway.security.allowed-email=daniele@example.com",
        "gateway.security.access-passphrase=my-secret-passphrase"
})
@AutoConfigureMockMvc
class ApiGatewayIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GatewayJwtTokenService jwtTokenService;

    @MockitoBean
    private GoogleOidcTokenVerifier googleOidcTokenVerifier;

    @MockitoBean
    private BackendProxyService backendProxyService;

    @Test
    @DisplayName("Sicurezza Gateway: Blocca con 401 le richieste non autenticate alle API REST")
    void shouldRejectUnauthenticatedCallsToProtectedRoutes() throws Exception {
        mockMvc.perform(get("/api/v1/expenses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Autenticazione richiesta"));
    }

    @Test
    @DisplayName("Login OIDC Google + Whitelist Email: Rifiuta con 403 account Google estranei e autorizza solo l'email in whitelist")
    void shouldEnforceEmailWhitelistOnGoogleOidcLogin() throws Exception {
        when(googleOidcTokenVerifier.verifyIdToken("intruder-google-token"))
                .thenReturn(Optional.of(new GoogleUserIdentity("intruder@gmail.com", "Intruder", true)));

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken": "intruder-google-token"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Account Google non in whitelist"));

        when(googleOidcTokenVerifier.verifyIdToken("daniele-google-token"))
                .thenReturn(Optional.of(new GoogleUserIdentity("daniele@example.com", "Daniele", true)));

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken": "daniele-google-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.userEmail").value("daniele@example.com"))
                .andExpect(jsonPath("$.authMethod").value("GOOGLE_OIDC"));
    }

    @Test
    @DisplayName("Login Passphrase + Proxy con M2M: Emette JWT valido e inoltra la richiesta autenticata a expense-service")
    void shouldAuthenticateAndProxyRequestToExpenseService() throws Exception {
        String validJwt = jwtTokenService.issueToken("daniele@example.com", "PASSPHRASE");
        assertThat(jwtTokenService.verifyToken(validJwt)).isPresent();

        ResponseEntity<byte[]> mockedDownstream = ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"totalElements\":95}".getBytes(StandardCharsets.UTF_8));
        when(backendProxyService.forwardToExpenseService(any(), any()))
                .thenAnswer(invocation -> mockedDownstream);

        mockMvc.perform(get("/api/v1/expenses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(95));

        verify(backendProxyService).forwardToExpenseService(any(), any());
    }
}
