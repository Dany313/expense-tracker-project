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
  return (
    <section className="surface-card" aria-label="Filtri di ricerca spese">
      <div className="surface-card-header">
        <h2 className="surface-card-title">Filtra Archivio</h2>
        <button type="button" className="btn btn-ghost btn-sm" onClick={onReset}>
          Reimposta filtri
        </button>
      </div>

      <div className="filters-grid">
        <div className="field-group">
          <label htmlFor="filter-store" className="field-label">
            Negozio
          </label>
          <input
            id="filter-store"
            type="search"
            className="input-control"
            placeholder="Cerca CONAD, ALDI..."
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
            Dal giorno
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
          <label htmlFor="filter-end-date" className="field-label">
            Al giorno
          </label>
          <input
            id="filter-end-date"
            type="date"
            className="input-control"
            value={filters.endDate ?? ''}
            onChange={(e) =>
              onChange({ ...filters, endDate: e.target.value, page: 0 })
            }
          />
        </div>

        <div className="field-group">
          <label htmlFor="filter-shared" className="field-label">
            Condivisione
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
            <option value="true">👥 Solo condivise</option>
            <option value="false">Solo personali</option>
          </select>
        </div>
      </div>
    </section>
  );
}
