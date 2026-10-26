package com.daniele.apigateway.security;

import java.util.Optional;

public interface GoogleOidcTokenVerifier {

    record GoogleUserIdentity(String email, String name, boolean emailVerified) {
    }

    Optional<GoogleUserIdentity> verifyIdToken(String idToken);
}
