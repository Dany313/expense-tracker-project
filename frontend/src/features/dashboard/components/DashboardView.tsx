import { useSplitShared } from '../../../core/context/SplitSharedContext';
import { resolveCategoryLabel } from '../../../shared/utils/constants';
import { formatCurrency, formatDateIt } from '../../../shared/utils/formatters';
import type {
  CategoryMetadata,
  ExpenseResponse,
  ExpenseSummaryResponse,
} from '../../expenses/types/expense.types';
import { CategoryBreakdown } from './CategoryBreakdown';

interface DashboardViewProps {
  summary: ExpenseSummaryResponse | null;
  recentExpenses: ExpenseResponse[];
  categories: CategoryMetadata[];
  loading: boolean;
  onNavigateCreate: () => void;
  onNavigateArchive: () => void;
  onNavigateMonthly: () => void;
}

export function DashboardView({
  summary,
  recentExpenses,
  categories,
  loading,
  onNavigateCreate,
  onNavigateArchive,
  onNavigateMonthly,
}: DashboardViewProps) {
  const { splitShared, getEffectiveAmount } = useSplitShared();
  const topCategoryCode =
    summary && Object.keys(summary.amountByCategory).length > 0
      ? Object.entries(summary.amountByCategory).sort((a, b) => b[1] - a[1])[0][0]
      : null;

  return (
    <>
      <div className="page-header-block">
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <h1 className="page-title">Panoramica Finanziaria</h1>
            {splitShared && (
              <span className="badge badge-shared" title="Filtro globale attivo: importi e statistiche riflettono la quota al 50% sulle spese condivise">
                👥 Quota 50% Condivise
              </span>
            )}
          </div>
          <p className="page-description">
            Statistiche in tempo reale calcolate sullo storico spese
          </p>
        </div>
        <div className="toolbar-actions">
          <button type="button" className="btn btn-secondary" onClick={onNavigateMonthly}>
            📅 Spese Mensili
          </button>
          <button type="button" className="btn btn-secondary" onClick={onNavigateArchive}>
            Esplora Archivio
          </button>
          <button type="button" className="btn btn-primary" onClick={onNavigateCreate}>
            + Nuova Spesa
          </button>
        </div>
      </div>

      <section className="kpi-grid" aria-label="Indicatori chiave di spesa">
        <article className="kpi-card">
          <span className="kpi-label">Totale Speso</span>
          <p className="kpi-value">{formatCurrency(summary?.totalAmount)}</p>
          <span className="kpi-caption">Cumulativo sul periodo</span>
        </article>

        <article className="kpi-card">
          <span className="kpi-label">Spesa Mensile Media</span>
          <p className="kpi-value">{formatCurrency(summary?.averageMonthlyAmount)}</p>
          <span className="kpi-caption">Media ponderata per mese attivo</span>
        </article>

        <article className="kpi-card">
          <span className="kpi-label">Spesa Totale Mese Corrente</span>
          <p className="kpi-value">
            {formatCurrency(summary?.currentMonthTotalAmount)}
          </p>
          <span className="kpi-caption">Uscite registrate nel mese in corso</span>
        </article>

        <article className="kpi-card">
          <span className="kpi-label">Categoria Principale</span>
          <p className="kpi-value">
            {topCategoryCode ? resolveCategoryLabel(topCategoryCode, categories) : '—'}
          </p>
          <span className="kpi-caption">Maggiore incidenza sul budget</span>
        </article>
      </section>

      <div className="dashboard-split-grid">
        <section className="surface-card" aria-labelledby="recent-expenses-heading">
          <div className="surface-card-header">
            <h2 id="recent-expenses-heading" className="surface-card-title">
              Ultimi Movimenti
            </h2>
            <button
              type="button"
              className="btn btn-ghost btn-sm"
              onClick={onNavigateArchive}
            >
              Vedi tutti →
            </button>
          </div>

          {loading && recentExpenses.length === 0 ? (
            <div className="empty-state">
              <p>Caricamento ultimi movimenti...</p>
            </div>
          ) : recentExpenses.length === 0 ? (
            <div className="empty-state">
              <p>Nessun movimento registrato.</p>
            </div>
          ) : (
            <div className="table-scroll-wrapper">
              <table className="expense-table">
                <thead>
                  <tr>
                    <th>Data</th>
                    <th>Negozio</th>
                    <th>Categoria</th>
                    <th className="text-end">Importo</th>
                  </tr>
                </thead>
                <tbody>
                  {recentExpenses.slice(0, 6).map((item) => (
                    <tr key={item.id}>
                      <td>{formatDateIt(item.expenseDate)}</td>
                      <td>
                        <div>
                          <strong>{item.storeName}</strong>
                        </div>
                        {item.shared && (
                          <div style={{ marginTop: '0.2rem' }}>
                            <span className="badge badge-shared" title="Spesa condivisa al 50%">
                              👥 Condivisa
                            </span>
                          </div>
                        )}
                      </td>
                      <td>
                        <span className="badge">
                          {item.categoryDisplayName || item.category}
                        </span>
                      </td>
                      <td className="amount-cell text-end">
                        <div>
                          <strong>{formatCurrency(getEffectiveAmount(item.amount, item.shared))}</strong>
                          {splitShared && item.shared && (
                            <div className="text-muted" style={{ fontSize: '0.75rem', fontWeight: 400 }}>
                              tot. {formatCurrency(item.amount)}
                            </div>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        <section className="surface-card" aria-labelledby="category-breakdown-heading">
          <div className="surface-card-header">
            <h2 id="category-breakdown-heading" className="surface-card-title">
              Distribuzione per Categoria
            </h2>
          </div>

          <CategoryBreakdown
            amountByCategory={summary?.amountByCategory ?? {}}
            totalAmount={summary?.totalAmount ?? 0}
            categories={categories}
          />
        </section>
      </div>
    </>
  );
}
