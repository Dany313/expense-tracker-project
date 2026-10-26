import React, { useState } from 'react';
import { ApiError } from '../../../core/api/httpClient';
import { useAuth } from '../../../core/auth/useAuth';
import { StatusBanner } from '../../../shared/components/StatusBanner';
import { useFormValidity } from '../../../shared/hooks/useFormValidity';
import { useTheme } from '../../../theme/ThemeContext';

export function LoginPage() {
  const { loginWithPassphrase, loginWithGoogleToken, authError, clearAuthError } = useAuth();
  const { theme, toggleTheme } = useTheme();

  const [authMode, setAuthMode] = useState<'passphrase' | 'oidc'>('passphrase');
  const [passphrase, setPassphrase] = useState<string>('');
  const [showPassword, setShowPassword] = useState<boolean>(false);
  const [googleIdToken, setGoogleIdToken] = useState<string>('');
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [localError, setLocalError] = useState<string | null>(null);

  const formRef = useFormValidity<HTMLFormElement>(authMode);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setLocalError(null);
    clearAuthError();
    setSubmitting(true);

    try {
      if (authMode === 'passphrase') {
        await loginWithPassphrase(passphrase.trim());
      } else {
        await loginWithGoogleToken(googleIdToken.trim());
      }
    } catch (err) {
      setLocalError(
        err instanceof ApiError
          ? err.message
          : 'Impossibile completare l’autenticazione sul Gateway.'
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-screen">
      <main className="auth-card" aria-labelledby="login-heading">
        <div className="surface-card-header">
          <div className="brand-header">
            <div className="brand-mark" aria-hidden="true">
              €
            </div>
            <div>
              <h1 id="login-heading" className="brand-title">
                Expense Tracker
              </h1>
              <p className="brand-subtitle">Accesso Protetto API Gateway</p>
            </div>
          </div>

          <button
            type="button"
            className="btn btn-ghost btn-sm"
            onClick={toggleTheme}
            aria-label="Cambia tema chiaro o scuro"
          >
            {theme === 'light' ? '🌙 Scuro' : '☀️ Chiaro'}
          </button>
        </div>

        <StatusBanner
          variant="error"
          message={localError ?? authError}
          onDismiss={() => {
            setLocalError(null);
            clearAuthError();
          }}
        />

        <div className="toolbar-actions" role="tablist" aria-label="Metodo di autenticazione">
          <button
            type="button"
            role="tab"
            aria-selected={authMode === 'passphrase'}
            className={`btn btn-sm ${
              authMode === 'passphrase' ? 'btn-primary' : 'btn-secondary'
            }`}
            onClick={() => setAuthMode('passphrase')}
          >
            Passphrase Personale
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={authMode === 'oidc'}
            className={`btn btn-sm ${authMode === 'oidc' ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setAuthMode('oidc')}
          >
            Google OIDC Token
          </button>
        </div>

        <form ref={formRef} className="form-stack" onSubmit={handleSubmit}>
          {authMode === 'passphrase' ? (
            <div className="field-group">
              <div className="field-label-row">
                <label htmlFor="current-password" className="field-label">
                  Passphrase di accesso
                </label>
              </div>
              <div className="input-with-action">
                <input
                  id="current-password"
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  className="input-control"
                  autoComplete="current-password"
                  enterKeyHint="done"
                  placeholder="Inserisci la tua passphrase"
                  required
                  value={passphrase}
                  onChange={(e) => setPassphrase(e.target.value)}
                />
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={() => setShowPassword((prev) => !prev)}
                  aria-label={showPassword ? 'Nascondi password' : 'Mostra password'}
                >
                  {showPassword ? 'Nascondi' : 'Mostra'}
                </button>
              </div>
              <span className="field-hint">
                Configurata in <code>GATEWAY_ACCESS_PASSPHRASE</code> nel file <code>.env</code>.
              </span>
            </div>
          ) : (
            <div className="field-group">
              <label htmlFor="google-oidc-token" className="field-label">
                Google OIDC ID Token (JWT)
              </label>
              <input
                id="google-oidc-token"
                name="idToken"
                type="text"
                className="input-control font-mono"
                spellCheck={false}
                autoCorrect="off"
                autoCapitalize="none"
                enterKeyHint="done"
                placeholder="eyJhbGciOiJSUzI1NiIs..."
                required
                value={googleIdToken}
                onChange={(e) => setGoogleIdToken(e.target.value)}
              />
              <span className="field-hint">
                Accetta solo token associati alla tua email in <code>ALLOWED_USER_EMAIL</code>.
              </span>
            </div>
          )}

          <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
            {submitting ? 'Verifica credenziali...' : 'Accedi all’Applicazione'}
          </button>
        </form>
      </main>
    </div>
  );
}
