import React, { createContext, useCallback, useEffect, useState } from 'react';
import { getStoredJwt, onUnauthorizedEvent, setStoredJwt } from '../api/httpClient';
import { authApi } from '../../features/auth/api/authApi';

export interface AuthContextValue {
  isCheckingSession: boolean;
  isAuthenticated: boolean;
  userEmail: string | null;
  authError: string | null;
  loginWithPassphrase: (passphrase: string) => Promise<void>;
  loginWithGoogleToken: (idToken: string) => Promise<void>;
  logout: () => void;
  clearAuthError: () => void;
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [isCheckingSession, setIsCheckingSession] = useState<boolean>(true);
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(() => Boolean(getStoredJwt()));
  const [userEmail, setUserEmail] = useState<string | null>(null);
  const [authError, setAuthError] = useState<string | null>(null);

  const verifySession = useCallback(async () => {
    setIsCheckingSession(true);
    try {
      const status = await authApi.getStatus();
      if (!status.authRequired || status.authenticated) {
        setIsAuthenticated(true);
        setUserEmail(status.userEmail ?? 'owner@expense-tracker');
      } else {
        setStoredJwt(null);
        setIsAuthenticated(false);
        setUserEmail(null);
      }
    } catch {
      // Fallback: if /api/v1/auth/status is unreachable (e.g. direct backend mode), check stored token
      setIsAuthenticated(Boolean(getStoredJwt()));
    } finally {
      setIsCheckingSession(false);
    }
  }, []);

  useEffect(() => {
    void verifySession();
  }, [verifySession]);

  useEffect(() => {
    return onUnauthorizedEvent(() => {
      setIsAuthenticated(false);
      setUserEmail(null);
      setAuthError('Sessione scaduta o non autorizzata. Effettua l’accesso per continuare.');
    });
  }, []);

  const loginWithPassphrase = async (passphrase: string) => {
    setAuthError(null);
    const tokenRes = await authApi.loginWithPassphrase(passphrase);
    setIsAuthenticated(true);
    setUserEmail(tokenRes.userEmail);
  };

  const loginWithGoogleToken = async (idToken: string) => {
    setAuthError(null);
    const tokenRes = await authApi.loginWithGoogleIdToken(idToken);
    setIsAuthenticated(true);
    setUserEmail(tokenRes.userEmail);
  };

  const logout = () => {
    setStoredJwt(null);
    setIsAuthenticated(false);
    setUserEmail(null);
    setAuthError(null);
  };

  const clearAuthError = () => setAuthError(null);

  return (
    <AuthContext.Provider
      value={{
        isCheckingSession,
        isAuthenticated,
        userEmail,
        authError,
        loginWithPassphrase,
        loginWithGoogleToken,
        logout,
        clearAuthError,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
