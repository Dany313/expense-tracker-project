import React from 'react';
import { useAuth } from '../auth/useAuth';
import { useSplitShared } from '../context/SplitSharedContext';
import { useTheme } from '../../theme/ThemeContext';

export type AppSection = 'dashboard' | 'monthly' | 'archive' | 'create' | 'assistant';

interface AppLayoutProps {
  activeSection: AppSection;
  onSelectSection: (section: AppSection) => void;
  onRefresh: () => void;
  loading: boolean;
  children: React.ReactNode;
}

const NAV_ITEMS: { id: AppSection; label: string; icon: string }[] = [
  { id: 'dashboard', label: 'Panoramica', icon: '📊' },
  { id: 'monthly', label: 'Spese Mensili', icon: '📅' },
  { id: 'archive', label: 'Archivio', icon: '🧾' },
  { id: 'create', label: 'Nuova Spesa', icon: '➕' },
  { id: 'assistant', label: 'AI & OCR', icon: '🤖' },
];

export function AppLayout({
  activeSection,
  onSelectSection,
  onRefresh,
  loading,
  children,
}: AppLayoutProps) {
  const { userEmail, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const { splitShared, toggleSplitShared } = useSplitShared();

  return (
    <div className="app-shell">
      {/* Top Navbar Unificata */}
      <header className="top-navbar" aria-label="Navigazione principale">
        <div className="top-navbar-inner">
          {/* Sinistra: Brand cliccabile per tornare alla Home */}
          <div className="navbar-left">
            <button
              type="button"
              className="brand-logo-btn"
              onClick={() => onSelectSection('dashboard')}
              title="Torna alla Panoramica"
            >
              <div className="brand-icon" aria-hidden="true">
                €
              </div>
              <div>
                <span className="brand-title">Expense</span>
                <span className="brand-badge"> Tracker</span>
              </div>
            </button>

            {/* Desktop Navigation Links */}
            <nav className="navbar-nav" aria-label="Sezioni">
              {NAV_ITEMS.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  className="nav-pill"
                  aria-current={activeSection === item.id ? 'page' : undefined}
                  onClick={() => onSelectSection(item.id)}
                >
                  <span aria-hidden="true">{item.icon}</span>
                  <span>{item.label}</span>
                </button>
              ))}
            </nav>
          </div>

          {/* Destra: Azioni globali e Utente */}
          <div className="navbar-right">
            {/* Toggle Quota Condivisa 50% */}
            <button
              type="button"
              className={`btn btn-sm ${splitShared ? 'btn-primary' : 'btn-secondary'}`}
              onClick={toggleSplitShared}
              aria-pressed={splitShared}
              title={
                splitShared
                  ? 'Filtro 50% attivo: le spese condivise sono dimezzate. Clicca per vedere importi interi.'
                  : 'Clicca per dimezzare al 50% gli importi delle spese condivise'
              }
            >
              <span aria-hidden="true">👥</span>
              <span>{splitShared ? 'Quota 50%' : 'Tutte 100%'}</span>
            </button>

            {/* Switch Tema Chiaro/Scuro */}
            <button
              type="button"
              className="btn btn-ghost btn-sm"
              onClick={toggleTheme}
              aria-label="Cambia tema chiaro o scuro"
              title={theme === 'light' ? 'Passa al tema scuro' : 'Passa al tema chiaro'}
            >
              {theme === 'light' ? '🌙' : '☀️'}
            </button>

            {/* Aggiorna */}
            <button
              type="button"
              className="btn btn-ghost btn-sm"
              onClick={onRefresh}
              disabled={loading}
              title="Aggiorna dati"
            >
              {loading ? '...' : '↻'}
            </button>

            {/* Logout */}
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={logout}
              title={`Disconnetti (${userEmail || 'sessione attiva'})`}
            >
              Esci
            </button>
          </div>
        </div>

        {/* Mobile Navigation Strip (scorre orizzontalmente sotto la navbar, zero invasività sul fondo) */}
        <nav className="mobile-nav-strip" aria-label="Navigazione mobile">
          {NAV_ITEMS.map((item) => (
            <button
              key={item.id}
              type="button"
              className="nav-pill"
              aria-current={activeSection === item.id ? 'page' : undefined}
              onClick={() => onSelectSection(item.id)}
            >
              <span aria-hidden="true">{item.icon}</span>
              <span>{item.label}</span>
            </button>
          ))}
        </nav>
      </header>

      {/* Main Centered Content */}
      <main className="main-container">{children}</main>
    </div>
  );
}
