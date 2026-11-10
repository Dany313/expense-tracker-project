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
          : 'Impossibile completare l’autenticazione.'
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-screen">
      <main className="auth-card" aria-labelledby="login-heading">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
            <div className="brand-icon" aria-hidden="true">
              €
            </div>
            <div>
              <h1 id="login-heading" className="brand-title">
                Expense Tracker
              </h1>
              <span className="brand-badge">Accesso Riservato</span>
            </div>
          </div>

          <button
            type="button"
            className="btn btn-ghost btn-sm"
            onClick={toggleTheme}
            aria-label="Cambia tema chiaro o scuro"
          >
            {theme === 'light' ? '🌙' : '☀️'}
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

        {/* Tab Scelta Metodo */}
        <div
          role="tablist"
          aria-label="Metodo di autenticazione"
          style={{
            display: 'flex',
            gap: '0.25rem',
            background: 'var(--bg-surface-subtle)',
            padding: '0.25rem',
            borderRadius: 'var(--radius-md)',
          }}
        >
          <button
            type="button"
            role="tab"
            aria-selected={authMode === 'passphrase'}
            className={`btn btn-sm ${
              authMode === 'passphrase' ? 'btn-primary' : 'btn-ghost'
            }`}
            style={{ flex: 1 }}
            onClick={() => setAuthMode('passphrase')}
          >
            Passphrase
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={authMode === 'oidc'}
            className={`btn btn-sm ${authMode === 'oidc' ? 'btn-primary' : 'btn-ghost'}`}
            style={{ flex: 1 }}
            onClick={() => setAuthMode('oidc')}
          >
            Google OIDC
          </button>
        </div>

        <form ref={formRef} className="form-stack" onSubmit={handleSubmit}>
          {authMode === 'passphrase' ? (
            <div className="field-group">
              <label htmlFor="current-password" className="field-label">
                Passphrase personale
              </label>
              <div className="input-with-action">
                <input
                  id="current-password"
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  className="input-control"
                  autoComplete="current-password"
                  enterKeyHint="done"
                  placeholder="Inserisci la passphrase"
                  required
                  value={passphrase}
                  onChange={(e) => setPassphrase(e.target.value)}
                />
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={() => setShowPassword((prev) => !prev)}
                  aria-label={showPassword ? 'Nascondi' : 'Mostra'}
                >
                  {showPassword ? '🙈' : '👁️'}
                </button>
              </div>
            </div>
          ) : (
            <div className="field-group">
              <label htmlFor="google-oidc-token" className="field-label">
                Google OIDC Token (JWT)
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
            </div>
          )}

          <button
            type="submit"
            className="btn btn-primary btn-block"
            disabled={submitting}
            style={{ marginTop: '0.25rem' }}
          >
            {submitting ? 'Verifica in corso...' : 'Accedi all’Applicazione'}
          </button>
        </form>
      </main>
    </div>
  );
}
