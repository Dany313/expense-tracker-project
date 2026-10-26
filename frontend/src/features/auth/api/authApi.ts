import { httpRequest, setStoredJwt } from '../../../core/api/httpClient';
import type { AuthStatusResponse, AuthTokenResponse } from '../types/auth.types';

const AUTH_BASE = import.meta.env.VITE_AUTH_BASE_URL ?? '/api/v1/auth';

export const authApi = {
  async getStatus(): Promise<AuthStatusResponse> {
    return httpRequest<AuthStatusResponse>(`${AUTH_BASE}/status`, {}, true);
  },

  async loginWithPassphrase(passphrase: string): Promise<AuthTokenResponse> {
    const response = await httpRequest<AuthTokenResponse>(
      `${AUTH_BASE}/login`,
      {
        method: 'POST',
        body: JSON.stringify({ passphrase }),
      },
      true
    );
    setStoredJwt(response.accessToken);
    return response;
  },

  async loginWithGoogleIdToken(idToken: string): Promise<AuthTokenResponse> {
    const response = await httpRequest<AuthTokenResponse>(
      `${AUTH_BASE}/google`,
      {
        method: 'POST',
        body: JSON.stringify({ idToken }),
      },
      true
    );
    setStoredJwt(response.accessToken);
    return response;
  },
};
