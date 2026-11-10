import type {
  CategoryMetadata,
  ExpenseCategoryCode,
  ExpenseFilterParams,
} from '../types/expense.types';

interface ExpenseFiltersBarProps {
  filters: ExpenseFilterParams;
  categories: CategoryMetadata[];
  onChange: (next: ExpenseFilterParams) => void;
  onReset: () => void;
}

export function ExpenseFiltersBar({
  filters,
  categories,
  onChange,
  onReset,
}: ExpenseFiltersBarProps) {
  const hasActiveFilters = Boolean(
    filters.storeName ||
    filters.category ||
    filters.startDate ||
    filters.endDate ||
    filters.shared !== undefined
  );

  return (
    <section className="surface-card" aria-label="Filtri di ricerca spese">
      <div className="surface-card-header">
        <h2 className="surface-card-title">Filtri di Ricerca</h2>
        {hasActiveFilters && (
          <button type="button" className="btn btn-ghost btn-sm" onClick={onReset}>
            Azzera filtri
          </button>
        )}
      </div>

      <div className="filters-grid">
        <div className="field-group">
          <label htmlFor="filter-store" className="field-label">
            Cerca negozio
          </label>
          <input
            id="filter-store"
            type="search"
            className="input-control"
            placeholder="es. CONAD, ALDI, ENI..."
            enterKeyHint="search"
            value={filters.storeName ?? ''}
            onChange={(e) =>
              onChange({ ...filters, storeName: e.target.value, page: 0 })
            }
          />
        </div>

        <div className="field-group">
          <label htmlFor="filter-category" className="field-label">
            Categoria
          </label>
          <select
            id="filter-category"
            className="input-control"
            value={filters.category ?? ''}
            onChange={(e) =>
              onChange({
                ...filters,
                category: e.target.value as ExpenseCategoryCode | '',
                page: 0,
              })
            }
          >
            <option value="">Tutte le categorie</option>
            {categories.map((cat) => (
              <option key={cat.code} value={cat.code}>
                {cat.displayName}
              </option>
            ))}
          </select>
        </div>

        <div className="field-group">
          <label htmlFor="filter-start-date" className="field-label">
            Data iniziale
          </label>
          <input
            id="filter-start-date"
            type="date"
            className="input-control"
            value={filters.startDate ?? ''}
            onChange={(e) =>
              onChange({ ...filters, startDate: e.target.value, page: 0 })
            }
          />
        </div>

        <div className="field-group">
          <label htmlFor="filter-shared" className="field-label">
            Tipo spesa
          </label>
          <select
            id="filter-shared"
            className="input-control"
            value={filters.shared ?? ''}
            onChange={(e) =>
              onChange({
                ...filters,
                shared: e.target.value || undefined,
                page: 0,
              })
            }
          >
            <option value="">Tutte le spese</option>
            <option value="true">👥 Solo condivise (50%)</option>
            <option value="false">👤 Solo personali (100%)</option>
          </select>
        </div>
      </div>
    </section>
  );
}
