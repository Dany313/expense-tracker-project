import { StatusBanner } from '../../../shared/components/StatusBanner';
import { useSplitShared } from '../../../core/context/SplitSharedContext';
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
      ? 'Tutte le categorie incluse'
      : selectedCategories
          .map((code) => resolveCategoryLabel(code, categories))
          .join(', ');

  return (
    <>
      <div className="page-header-block">
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <h1 className="page-title">Riepilogo Spese Mensili</h1>
            {splitShared && (
              <span className="badge badge-shared" title="Filtro globale attivo: importi e statistiche mensili riflettono la quota al 50% sulle spese condivise">
                👥 Quota 50% Condivise
              </span>
            )}
          </div>
          <p className="page-description">
            Andamento mese per mese del totale speso, con selezione multipla per categorie
          </p>
        </div>
      </div>

      <StatusBanner variant="error" message={error} />

      {/* Selezione Multipla Categorie tramite Chip + Filtro Anno */}
      <section className="surface-card" aria-label="Filtri analisi mensile">
        <div className="surface-card-header">
          <div>
            <h2 className="surface-card-title">Categorie da Includere</h2>
            <p className="field-hint">
              Seleziona una o più chip contemporaneamente per sommare le relative spese mensili
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
              Azzera filtri
            </button>
          )}
        </div>

        <div className="chip-group" role="group" aria-label="Filtra per una o più categorie">
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

        {availableYears.length > 0 && (
          <div className="field-group">
            <span className="field-label">Anno solare</span>
            <div className="chip-group" role="group" aria-label="Filtra per anno solare">
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

      {/* KPI del Filtro Mensile Selezionato */}
      <section className="kpi-grid" aria-label="Indicatori mensili per il filtro attivo">
        <article className="kpi-card">
          <span className="kpi-label">Totale Periodo Filtrato</span>
          <p className="kpi-value">{formatCurrency(totalPeriodAmount)}</p>
          <span className="kpi-caption">{selectedCategoriesCaption}</span>
        </article>

        <article className="kpi-card">
          <span className="kpi-label">Media Mensile</span>
          <p className="kpi-value">{formatCurrency(averageMonthlyAmount)}</p>
          <span className="kpi-caption">Calcolata su {monthlyRows.length} mesi attivi</span>
        </article>

        <article className="kpi-card">
          <span className="kpi-label">Mese con Spesa Massima</span>
          <p className="kpi-value">
            {peakMonth ? formatCurrency(peakMonth.totalAmount) : '—'}
          </p>
          <span className="kpi-caption">
            {peakMonth ? peakMonth.monthLabel : 'Nessun dato'}
          </span>
        </article>

        <article className="kpi-card">
          <span className="kpi-label">Mesi con Movimenti</span>
          <p className="kpi-value">{monthlyRows.length}</p>
          <span className="kpi-caption">Mesi con almeno una spesa registrata</span>
        </article>
      </section>

      {/* Tabella Dettaglio Mese per Mese */}
      <section className="surface-card" aria-labelledby="monthly-table-heading">
        <div className="surface-card-header">
          <h2 id="monthly-table-heading" className="surface-card-title">
            Totale Spese per Singolo Mese
          </h2>
          {selectedCategories.length > 0 && (
            <span className="badge">
              {selectedCategories.length}{' '}
              {selectedCategories.length === 1
                ? 'categoria selezionata'
                : 'categorie selezionate'}
            </span>
          )}
        </div>

        {loading && monthlyRows.length === 0 ? (
          <div className="empty-state">
            <p>Calcolo totali mensili in corso...</p>
          </div>
        ) : monthlyRows.length === 0 ? (
          <div className="empty-state">
            <p>Nessuna spesa registrata per le categorie o l’anno selezionato.</p>
          </div>
        ) : (
          <div className="table-scroll-wrapper">
            <table className="expense-table">
              <thead>
                <tr>
                  <th>Mese</th>
                  <th>N. Spese</th>
                  <th>Categoria Prevalente</th>
                  <th>Confronto con il picco</th>
                  <th className="text-end">Totale Mese</th>
                </tr>
              </thead>
              <tbody>
                {monthlyRows.map((row) => (
                  <tr key={row.yearMonth}>
                    <td>
                      <strong>{row.monthLabel}</strong>
                    </td>
                    <td>{row.expenseCount} movimenti</td>
                    <td>
                      <span className="badge">
                        {resolveCategoryLabel(row.topCategory, categories)}
                      </span>
                    </td>
                    <td>
                      <progress
                        className="progress-bar"
                        value={row.shareOfPeakPercent}
                        max={100}
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
