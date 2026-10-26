export interface AuthTokenResponse {
  accessToken: string;
  tokenType: string;
  userEmail: string;
  authMethod: string;
  expiresInHours: number;
  issuedAt: string;
}

export interface AuthStatusResponse {
  authRequired: boolean;
  authenticated: boolean;
  userEmail: string | null;
  googleOidcConfigured: boolean;
  googleClientId: string;
}
