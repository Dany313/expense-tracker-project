import { useSplitShared } from '../../../core/context/SplitSharedContext';
import { EmptyState } from '../../../shared/components/EmptyState';
import { Skeleton } from '../../../shared/components/Skeleton';
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
}: DashboardViewProps) {
  const { splitShared, getEffectiveAmount } = useSplitShared();

  const topCategoryCode =
    summary && Object.keys(summary.amountByCategory).length > 0
      ? Object.entries(summary.amountByCategory).sort((a, b) => b[1] - a[1])[0][0]
      : null;

  return (
    <>
      {/* Header */}
      <div className="page-header-block">
        <div>
          <h1 className="page-title">Panoramica Finanziaria</h1>
          <p className="page-description">
            {splitShared
              ? 'Statistiche calcolate con quota al 50% sulle spese condivise'
              : 'Statistiche in tempo reale calcolate sull’intero storico spese'}
          </p>
        </div>
        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          <button type="button" className="btn btn-secondary" onClick={onNavigateArchive}>
            Archivio
          </button>
          <button type="button" className="btn btn-primary" onClick={onNavigateCreate}>
            + Nuova Spesa
          </button>
        </div>
      </div>

      {/* KPI Cards */}
      <section className="kpi-grid" aria-label="Indicatori chiave di spesa">
        <article className="kpi-card">
          <span className="kpi-label">Totale Speso</span>
          {loading && !summary ? (
            <Skeleton height="2rem" />
          ) : (
            <p className="kpi-value">{formatCurrency(summary?.totalAmount)}</p>
          )}
          <span className="kpi-caption">Cumulativo registrato</span>
        </article>

        <article className="kpi-card">
          <span className="kpi-label">Media Mensile</span>
          {loading && !summary ? (
            <Skeleton height="2rem" />
          ) : (
            <p className="kpi-value">{formatCurrency(summary?.averageMonthlyAmount)}</p>
          )}
          <span className="kpi-caption">Spesa media per mese attivo</span>
        </article>

        <article className="kpi-card">
          <span className="kpi-label">Mese Corrente</span>
          {loading && !summary ? (
            <Skeleton height="2rem" />
          ) : (
            <p className="kpi-value">
              {formatCurrency(summary?.currentMonthTotalAmount)}
            </p>
          )}
          <span className="kpi-caption">Uscite del mese in corso</span>
        </article>

        <article className="kpi-card">
          <span className="kpi-label">Top Categoria</span>
          {loading && !summary ? (
            <Skeleton height="2rem" />
          ) : (
            <p className="kpi-value" style={{ fontSize: '1.15rem' }}>
              {topCategoryCode ? resolveCategoryLabel(topCategoryCode, categories) : '—'}
            </p>
          )}
          <span className="kpi-caption">Maggiore incidenza di spesa</span>
        </article>
      </section>

      {/* Main Split Grid */}
      <div className="dashboard-split-grid">
        {/* Ultimi Movimenti */}
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
              Vedi tutti ({summary?.totalCount ?? recentExpenses.length}) →
            </button>
          </div>

          {loading && recentExpenses.length === 0 ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              <Skeleton height="3rem" />
              <Skeleton height="3rem" />
              <Skeleton height="3rem" />
            </div>
          ) : recentExpenses.length === 0 ? (
            <EmptyState
              icon="💳"
              title="Nessun movimento presente"
              description="Inizia registrando la tua prima spesa manuale o caricando la foto di uno scontrino."
              action={{
                label: "+ Aggiungi Spesa",
                onClick: onNavigateCreate,
              }}
            />
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
                      <td style={{ whiteSpace: 'nowrap' }}>
                        {formatDateIt(item.expenseDate)}
                      </td>
                      <td>
                        <strong>{item.storeName}</strong>
                        {item.shared && (
                          <span
                            className="badge badge-shared"
                            style={{ marginLeft: '0.4rem' }}
                            title="Spesa condivisa al 50%"
                          >
                            50%
                          </span>
                        )}
                      </td>
                      <td>
                        <span className="badge">
                          {item.categoryDisplayName || item.category}
                        </span>
                      </td>
                      <td className="amount-cell text-end">
                        <div>
                          {formatCurrency(getEffectiveAmount(item.amount, item.shared))}
                        </div>
                        {splitShared && item.shared && (
                          <div
                            className="text-muted"
                            style={{ fontSize: '0.72rem', fontWeight: 400 }}
                          >
                            tot. {formatCurrency(item.amount)}
                          </div>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        {/* Distribuzione per Categoria */}
        <section className="surface-card" aria-labelledby="category-breakdown-heading">
          <div className="surface-card-header">
            <h2 id="category-breakdown-heading" className="surface-card-title">
              Distribuzione Spese
            </h2>
          </div>

          {loading && !summary ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <Skeleton height="1.5rem" />
              <Skeleton height="1.5rem" />
              <Skeleton height="1.5rem" />
            </div>
          ) : (
            <CategoryBreakdown
              amountByCategory={summary?.amountByCategory ?? {}}
              totalAmount={summary?.totalAmount ?? 0}
              categories={categories}
            />
          )}
        </section>
      </div>
    </>
  );
}
