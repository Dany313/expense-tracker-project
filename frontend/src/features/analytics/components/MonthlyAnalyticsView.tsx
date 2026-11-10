import { useSplitShared } from '../../../core/context/SplitSharedContext';
import { EmptyState } from '../../../shared/components/EmptyState';
import { Skeleton } from '../../../shared/components/Skeleton';
import { StatusBanner } from '../../../shared/components/StatusBanner';
import { resolveCategoryLabel } from '../../../shared/utils/constants';
import { formatCurrency } from '../../../shared/utils/formatters';
import type { CategoryMetadata } from '../../expenses/types/expense.types';
import { useMonthlyAnalytics } from '../hooks/useMonthlyAnalytics';

interface MonthlyAnalyticsViewProps {
  categories: CategoryMetadata[];
}

export function MonthlyAnalyticsView({ categories }: MonthlyAnalyticsViewProps) {
  const { splitShared } = useSplitShared();
  const {
    selectedCategories,
    toggleCategory,
    clearCategories,
    selectedYear,
    setSelectedYear,
    availableYears,
    monthlyRows,
    totalPeriodAmount,
    averageMonthlyAmount,
    peakMonth,
    loading,
    error,
  } = useMonthlyAnalytics(true);

  const hasActiveFilters = selectedCategories.length > 0 || Boolean(selectedYear);

  const selectedCategoriesCaption =
    selectedCategories.length === 0
      ? 'Tutte le categorie'
      : selectedCategories
          .map((code) => resolveCategoryLabel(code, categories))
          .join(', ');

  return (
    <>
      {/* Header */}
      <div className="page-header-block">
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
            <h1 className="page-title">Spese Mensili</h1>
            {splitShared && (
              <span className="badge badge-shared" title="Filtro globale quota 50% attivo">
                👥 Quota 50%
              </span>
            )}
          </div>
          <p className="page-description">
            Andamento aggregato mese per mese con filtro multi-categoria
          </p>
        </div>
      </div>

      <StatusBanner variant="error" message={error} />

      {/* Filtri Categorie & Anno */}
      <section className="surface-card" aria-label="Filtri per categoria e anno">
        <div className="surface-card-header">
          <div>
            <h2 className="surface-card-title">Filtra per Categoria</h2>
            <p className="field-hint" style={{ marginTop: '0.2rem' }}>
              Tocca le categorie per includerle o escluderle dal calcolo
            </p>
          </div>
          {hasActiveFilters && (
            <button
              type="button"
              className="btn btn-ghost btn-sm"
              onClick={() => {
                clearCategories();
                setSelectedYear('');
              }}
            >
              Azzera selezione
            </button>
          )}
        </div>

        {/* Chips Categorie */}
        <div className="chip-group" role="group" aria-label="Filtro categorie">
          <button
            type="button"
            className="filter-chip"
            aria-pressed={selectedCategories.length === 0}
            onClick={clearCategories}
          >
            Tutte le categorie
          </button>

          {categories.map((cat) => {
            const isSelected = selectedCategories.includes(cat.code);
            return (
              <button
                key={cat.code}
                type="button"
                className="filter-chip"
                aria-pressed={isSelected}
                onClick={() => toggleCategory(cat.code)}
              >
                <span>{isSelected ? '✓' : '+'}</span>
                <span>{cat.displayName}</span>
              </button>
            );
          })}
        </div>

        {/* Chips Anno se presenti più anni */}
        {availableYears.length > 0 && (
          <div style={{ marginTop: '0.5rem', paddingTop: '0.75rem', borderTop: '1px solid var(--border-subtle)' }}>
            <span className="field-label" style={{ display: 'block', marginBottom: '0.4rem' }}>
              Anno solare
            </span>
            <div className="chip-group" role="group" aria-label="Filtro anno">
              <button
                type="button"
                className="filter-chip"
                aria-pressed={selectedYear === ''}
                onClick={() => setSelectedYear('')}
              >
                Tutti gli anni
              </button>
              {availableYears.map((yr) => (
                <button
                  key={yr}
                  type="button"
                  className="filter-chip"
                  aria-pressed={selectedYear === yr}
                  onClick={() => setSelectedYear(selectedYear === yr ? '' : yr)}
                >
                  {yr}
                </button>
              ))}
            </div>
          </div>
        )}
      </section>

      {/* KPI del periodo */}
      <section className="kpi-grid" aria-label="Indicatori del periodo">
        <article className="kpi-card">
          <span className="kpi-label">Totale Periodo</span>
          {loading && monthlyRows.length === 0 ? (
            <Skeleton height="2rem" />
          ) : (
            <p className="kpi-value">{formatCurrency(totalPeriodAmount)}</p>
          )}
          <span className="kpi-caption">{selectedCategoriesCaption}</span>
        </article>

        <article className="kpi-card">
          <span className="kpi-label">Media Mensile</span>
          {loading && monthlyRows.length === 0 ? (
            <Skeleton height="2rem" />
          ) : (
            <p className="kpi-value">{formatCurrency(averageMonthlyAmount)}</p>
          )}
          <span className="kpi-caption">Calcolata su {monthlyRows.length} mesi</span>
        </article>

        <article className="kpi-card">
          <span className="kpi-label">Picco Mensile</span>
          {loading && monthlyRows.length === 0 ? (
            <Skeleton height="2rem" />
          ) : (
            <p className="kpi-value">
              {peakMonth ? formatCurrency(peakMonth.totalAmount) : '—'}
            </p>
          )}
          <span className="kpi-caption">
            {peakMonth ? peakMonth.monthLabel : 'Nessun picco'}
          </span>
        </article>

        <article className="kpi-card">
          <span className="kpi-label">Mesi Registrati</span>
          {loading && monthlyRows.length === 0 ? (
            <Skeleton height="2rem" />
          ) : (
            <p className="kpi-value">{monthlyRows.length}</p>
          )}
          <span className="kpi-caption">Mesi con almeno una spesa</span>
        </article>
      </section>

      {/* Tabella mensile */}
      <section className="surface-card" aria-labelledby="monthly-table-heading">
        <div className="surface-card-header">
          <h2 id="monthly-table-heading" className="surface-card-title">
            Dettaglio Mese per Mese
          </h2>
          {selectedCategories.length > 0 && (
            <span className="badge">
              {selectedCategories.length} {selectedCategories.length === 1 ? 'categoria' : 'categorie'}
            </span>
          )}
        </div>

        {loading && monthlyRows.length === 0 ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            <Skeleton height="3rem" />
            <Skeleton height="3rem" />
            <Skeleton height="3rem" />
          </div>
        ) : monthlyRows.length === 0 ? (
          <EmptyState
            icon="📅"
            title="Nessun dato per il filtro selezionato"
            description="Prova a selezionare un anno differente o includere tutte le categorie."
            action={{
              label: "Includi tutte le categorie",
              onClick: clearCategories,
            }}
          />
        ) : (
          <div className="table-scroll-wrapper">
            <table className="expense-table">
              <thead>
                <tr>
                  <th>Mese</th>
                  <th>Movimenti</th>
                  <th>Categoria Principale</th>
                  <th>Grafico incidenza</th>
                  <th className="text-end">Totale Mese</th>
                </tr>
              </thead>
              <tbody>
                {monthlyRows.map((row) => (
                  <tr key={row.yearMonth}>
                    <td style={{ whiteSpace: 'nowrap' }}>
                      <strong>{row.monthLabel}</strong>
                    </td>
                    <td>{row.expenseCount} spese</td>
                    <td>
                      <span className="badge">
                        {resolveCategoryLabel(row.topCategory, categories)}
                      </span>
                    </td>
                    <td style={{ minWidth: '8rem' }}>
                      <progress
                        className="progress-bar"
                        value={row.shareOfPeakPercent}
                        max={100}
                        title={`${row.shareOfPeakPercent}% del mese di picco`}
                      />
                    </td>
                    <td className="amount-cell text-end">
                      {formatCurrency(row.totalAmount)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  );
}
