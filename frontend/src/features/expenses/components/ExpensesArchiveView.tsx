import { useState } from 'react';
import { useSplitShared } from '../../../core/context/SplitSharedContext';
import { formatCurrency, formatDateIt } from '../../../shared/utils/formatters';
import type {
  CategoryMetadata,
  ExpenseCreateRequest,
  ExpenseFilterParams,
  ExpenseResponse,
} from '../types/expense.types';
import { EditExpenseModal } from './EditExpenseModal';
import { ExpenseFiltersBar } from './ExpenseFiltersBar';

interface ExpensesArchiveViewProps {
  expenses: ExpenseResponse[];
  categories: CategoryMetadata[];
  filters: ExpenseFilterParams;
  totalElements: number;
  totalPages: number;
  loading: boolean;
  submitting: boolean;
  onChangeFilters: (next: ExpenseFilterParams) => void;
  onResetFilters: () => void;
  onReplaceExpense: (id: string, payload: ExpenseCreateRequest) => Promise<boolean>;
  onDeleteExpense: (expense: ExpenseResponse) => Promise<void>;
  onNavigateCreate: () => void;
}

export function ExpensesArchiveView({
  expenses,
  categories,
  filters,
  totalElements,
  totalPages,
  loading,
  submitting,
  onChangeFilters,
  onResetFilters,
  onReplaceExpense,
  onDeleteExpense,
  onNavigateCreate,
}: ExpensesArchiveViewProps) {
  const { splitShared, getEffectiveAmount } = useSplitShared();
  const [editingExpense, setEditingExpense] = useState<ExpenseResponse | null>(null);
  const currentPage = filters.page ?? 0;

  return (
    <>
      <div className="page-header-block">
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <h1 className="page-title">Archivio Spese</h1>
            {splitShared && (
              <span className="badge badge-shared" title="Filtro globale attivo: importi spese condivise conteggiati al 50%">
                👥 Quota 50%
              </span>
            )}
          </div>
          <p className="page-description">
            {totalElements} movimenti registrati su PostgreSQL
          </p>
        </div>
        <button type="button" className="btn btn-primary" onClick={onNavigateCreate}>
          + Nuova Spesa
        </button>
      </div>

      <ExpenseFiltersBar
        filters={filters}
        categories={categories}
        onChange={onChangeFilters}
        onReset={onResetFilters}
      />

      <section className="surface-card" aria-label="Elenco movimenti">
        {loading && expenses.length === 0 ? (
          <div className="empty-state">
            <p>Caricamento movimenti in corso...</p>
          </div>
        ) : expenses.length === 0 ? (
          <div className="empty-state">
            <p>Nessuna spesa corrisponde ai criteri di ricerca selezionati.</p>
          </div>
        ) : (
          <div className="table-scroll-wrapper">
            <table className="expense-table">
              <thead>
                <tr>
                  <th>Data</th>
                  <th>Negozio</th>
                  <th>Categoria</th>
                  <th>Note / Sottocategoria</th>
                  <th className="text-end">Importo</th>
                  <th className="text-end">Azioni</th>
                </tr>
              </thead>
              <tbody>
                {expenses.map((expense) => (
                  <tr key={expense.id}>
                    <td>{formatDateIt(expense.expenseDate)}</td>
                    <td>
                      <div>
                        <strong>{expense.storeName}</strong>
                      </div>
                      <div style={{ display: 'flex', gap: '0.25rem', marginTop: '0.25rem', flexWrap: 'wrap' }}>
                        <button
                          type="button"
                          className={`badge ${expense.shared ? 'badge-shared' : 'badge-ghost'}`}
                          title={
                            expense.shared
                              ? 'Spesa condivisa al 50%. Clicca per impostarla come personale'
                              : 'Spesa personale. Clicca per impostarla come condivisa al 50%'
                          }
                          onClick={() => {
                            void onReplaceExpense(expense.id, {
                              expenseDate: expense.expenseDate,
                              storeName: expense.storeName,
                              amount: expense.amount,
                              category: expense.category,
                              subcategory: expense.subcategory,
                              description: expense.description,
                              recurring: expense.recurring,
                              shared: !expense.shared,
                            });
                          }}
                        >
                          {expense.shared ? '👥 Condivisa (50%)' : '👤 Personale'}
                        </button>
                        {expense.recurring && (
                          <span className="badge" title="Spesa periodica / ricorrente">
                            🔄 Ricorrente
                          </span>
                        )}
                      </div>
                    </td>
                    <td>
                      <span className="badge">
                        {expense.categoryDisplayName || expense.category}
                      </span>
                    </td>
                    <td className="text-muted">
                      {[expense.subcategory, expense.description]
                        .filter(Boolean)
                        .join(' — ') || '—'}
                    </td>
                    <td className="amount-cell text-end">
                      <div>
                        <strong>{formatCurrency(getEffectiveAmount(expense.amount, expense.shared))}</strong>
                        {splitShared && expense.shared && (
                          <div className="text-muted" style={{ fontSize: '0.75rem', fontWeight: 400 }}>
                            tot. {formatCurrency(expense.amount)}
                          </div>
                        )}
                      </div>
                    </td>
                    <td className="text-end">
                      <div className="row-actions">
                        <button
                          type="button"
                          className="btn btn-secondary btn-sm"
                          onClick={() => setEditingExpense(expense)}
                        >
                          Modifica
                        </button>
                        <button
                          type="button"
                          className="btn btn-danger btn-sm"
                          onClick={() => void onDeleteExpense(expense)}
                        >
                          Elimina
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <div className="pagination-bar">
          <span>
            Pagina <strong>{totalPages === 0 ? 0 : currentPage + 1}</strong> di{' '}
            <strong>{totalPages}</strong> ({totalElements} totali)
          </span>
          <div className="toolbar-actions">
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              disabled={currentPage <= 0 || loading}
              onClick={() =>
                onChangeFilters({ ...filters, page: Math.max(0, currentPage - 1) })
              }
            >
              ← Precedente
            </button>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              disabled={currentPage + 1 >= totalPages || loading}
              onClick={() => onChangeFilters({ ...filters, page: currentPage + 1 })}
            >
              Successiva →
            </button>
          </div>
        </div>
      </section>

      <EditExpenseModal
        expense={editingExpense}
        categories={categories}
        submitting={submitting}
        onClose={() => setEditingExpense(null)}
        onSave={onReplaceExpense}
      />
    </>
  );
}
