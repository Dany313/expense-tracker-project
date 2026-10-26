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

const NAV_ITEMS: { id: AppSection; label: string; shortLabel: string; icon: string }[] = [
  { id: 'dashboard', label: 'Panoramica', shortLabel: 'Home', icon: '📊' },
  { id: 'monthly', label: 'Spese Mensili', shortLabel: 'Mesi', icon: '📅' },
  { id: 'archive', label: 'Archivio Spese', shortLabel: 'Archivio', icon: '🧾' },
  { id: 'create', label: 'Nuova Spesa', shortLabel: 'Nuova', icon: '➕' },
  { id: 'assistant', label: 'Assistente AI & OCR', shortLabel: 'AI / OCR', icon: '🤖' },
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
    <div className="app-layout">
      {/* Desktop Persistent Sidebar */}
      <aside className="app-sidebar" aria-label="Menu principale">
        <div>
          <div className="brand-header">
            <div className="brand-mark" aria-hidden="true">
              €
            </div>
            <div>
              <div className="brand-title">Expense Tracker</div>
              <div className="brand-subtitle">Piattaforma Personale</div>
            </div>
          </div>

          <nav className="sidebar-nav" aria-label="Sezioni applicazione">
            {NAV_ITEMS.map((item) => (
              <button
                key={item.id}
                type="button"
                className="nav-item"
                aria-current={activeSection === item.id ? 'page' : undefined}
                onClick={() => onSelectSection(item.id)}
              >
                <span aria-hidden="true">{item.icon}</span>
                <span>{item.label}</span>
              </button>
            ))}
          </nav>
        </div>

        <div className="form-stack">
          {userEmail && <span className="badge badge-success">{userEmail}</span>}
          <button type="button" className="btn btn-secondary btn-sm" onClick={logout}>
            Esci dalla sessione
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="app-main-column">
        <header className="app-topbar">
          <div className="brand-header">
            <span className="brand-title">
              {NAV_ITEMS.find((i) => i.id === activeSection)?.label}
            </span>
          </div>

          <div className="toolbar-actions">
            <button
              type="button"
              className={`btn btn-sm ${splitShared ? 'btn-primary' : 'btn-secondary'}`}
              onClick={toggleSplitShared}
              aria-pressed={splitShared}
              title={
                splitShared
                  ? 'Quota 50% attiva: le spese condivise valgono la metà. Clicca per vedere gli importi interi.'
                  : 'Clicca per calcolare le spese condivise al 50%'
              }
            >
              👥 {splitShared ? 'Quota 50% Attiva' : 'Dimezza Condivise'}
            </button>

            <button
              type="button"
              className="btn btn-ghost btn-sm"
              onClick={toggleTheme}
              aria-label="Cambia tema chiaro o scuro"
            >
              {theme === 'light' ? '🌙 Scuro' : '☀️ Chiaro'}
            </button>

            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={onRefresh}
              disabled={loading}
            >
              {loading ? 'Sincronizzazione...' : '↻ Aggiorna'}
            </button>

            <button type="button" className="btn btn-ghost btn-sm" onClick={logout}>
              Esci
            </button>
          </div>
        </header>

        <main className="app-content-container">{children}</main>
      </div>

      {/* Mobile Bottom Navigation */}
      <nav className="mobile-bottom-nav" aria-label="Navigazione mobile">
        {NAV_ITEMS.map((item) => (
          <button
            key={item.id}
            type="button"
            className="mobile-nav-item"
            aria-current={activeSection === item.id ? 'page' : undefined}
            onClick={() => onSelectSection(item.id)}
          >
            <span aria-hidden="true">{item.icon}</span>
            <span>{item.shortLabel}</span>
          </button>
        ))}
      </nav>
    </div>
  );
}
