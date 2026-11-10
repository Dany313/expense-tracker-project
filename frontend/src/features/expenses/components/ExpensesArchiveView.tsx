import { useState } from 'react';
import { useSplitShared } from '../../../core/context/SplitSharedContext';
import { EmptyState } from '../../../shared/components/EmptyState';
import { Skeleton } from '../../../shared/components/Skeleton';
import { formatCurrency, formatDateIt } from '../../../shared/utils/formatters';
import type {
  CategoryMetadata,
  ExpenseCreateRequest,
  ExpenseFilterParams,
  ExpenseResponse,
} from '../types/expense.types';
import { EditExpenseModal } from './EditExpenseModal';
import { ExpenseFiltersBar } from './ExpenseFiltersBar';
import { ExpenseItemCard } from './ExpenseItemCard';

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

  const handleToggleShared = (expense: ExpenseResponse) => {
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
  };

  return (
    <>
      {/* Header */}
      <div className="page-header-block">
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
            <h1 className="page-title">Archivio Spese</h1>
            {splitShared && (
              <span className="badge badge-shared" title="Filtro quota 50% attivo sulle spese condivise">
                👥 Quota 50%
              </span>
            )}
          </div>
          <p className="page-description">
            {totalElements} movimenti contabili registrati
          </p>
        </div>
        <button type="button" className="btn btn-primary" onClick={onNavigateCreate}>
          + Nuova Spesa
        </button>
      </div>

      {/* Barra Filtri Fluida */}
      <ExpenseFiltersBar
        filters={filters}
        categories={categories}
        onChange={onChangeFilters}
        onReset={onResetFilters}
      />

      {/* Sezione Movimenti */}
      <section className="surface-card" aria-label="Elenco movimenti">
        {loading && expenses.length === 0 ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            <Skeleton height="3.5rem" />
            <Skeleton height="3.5rem" />
            <Skeleton height="3.5rem" />
            <Skeleton height="3.5rem" />
          </div>
        ) : expenses.length === 0 ? (
          <EmptyState
            icon="🔍"
            title="Nessuna spesa trovata"
            description="Nessun movimento corrisponde ai criteri o ai filtri selezionati."
            action={{
              label: "Azzera filtri",
              onClick: onResetFilters,
            }}
          />
        ) : (
          <>
            {/* Vista Mobile: Cards touch-friendly (nascoste su desktop con media query inline o classe) */}
            <div className="expense-cards-list mobile-only-block" style={{ display: 'flex', flexDirection: 'column', gap: '0.65rem' }}>
              {expenses.map((expense) => (
                <ExpenseItemCard
                  key={expense.id}
                  expense={expense}
                  effectiveAmount={getEffectiveAmount(expense.amount, expense.shared)}
                  splitShared={splitShared}
                  onEdit={() => setEditingExpense(expense)}
                  onDelete={() => void onDeleteExpense(expense)}
                  onToggleShared={() => handleToggleShared(expense)}
                />
              ))}
            </div>

            {/* Vista Desktop: Tabella compatta */}
            <div className="table-scroll-wrapper desktop-only-block">
              <table className="expense-table">
                <thead>
                  <tr>
                    <th>Data</th>
                    <th>Negozio</th>
                    <th>Categoria</th>
                    <th>Note / Dettagli</th>
                    <th className="text-end">Importo</th>
                    <th className="text-end">Azioni</th>
                  </tr>
                </thead>
                <tbody>
                  {expenses.map((expense) => {
                    const effective = getEffectiveAmount(expense.amount, expense.shared);
                    return (
                      <tr key={expense.id}>
                        <td style={{ whiteSpace: 'nowrap' }}>
                          {formatDateIt(expense.expenseDate)}
                        </td>
                        <td>
                          <div>
                            <strong>{expense.storeName}</strong>
                          </div>
                          <div style={{ display: 'flex', gap: '0.3rem', marginTop: '0.2rem' }}>
                            <button
                              type="button"
                              className={`badge ${expense.shared ? 'badge-shared' : 'badge-ghost'}`}
                              title="Clicca per invertire modalità personale/condivisa"
                              onClick={() => handleToggleShared(expense)}
                            >
                              {expense.shared ? '👥 50%' : '👤 100%'}
                            </button>
                            {expense.recurring && (
                              <span className="badge" title="Ricorrente">
                                🔄
                              </span>
                            )}
                          </div>
                        </td>
                        <td>
                          <span className="badge">
                            {expense.categoryDisplayName || expense.category}
                          </span>
                        </td>
                        <td className="text-muted" style={{ fontSize: '0.82rem' }}>
                          {[expense.subcategory, expense.description]
                            .filter(Boolean)
                            .join(' · ') || '—'}
                        </td>
                        <td className="amount-cell text-end">
                          <div>{formatCurrency(effective)}</div>
                          {splitShared && expense.shared && (
                            <div className="text-muted" style={{ fontSize: '0.72rem', fontWeight: 400 }}>
                              tot. {formatCurrency(expense.amount)}
                            </div>
                          )}
                        </td>
                        <td className="text-end">
                          <div style={{ display: 'inline-flex', gap: '0.35rem' }}>
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
                    );
                  })}
                </tbody>
              </table>
            </div>
          </>
        )}

        {/* Paginazione */}
        {totalPages > 1 && (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              paddingTop: '0.75rem',
              borderTop: '1px solid var(--border-subtle)',
              fontSize: '0.85rem',
              flexWrap: 'wrap',
              gap: '0.5rem',
            }}
          >
            <span>
              Pagina <strong>{totalPages === 0 ? 0 : currentPage + 1}</strong> di{' '}
              <strong>{totalPages}</strong> ({totalElements} totali)
            </span>
            <div style={{ display: 'flex', gap: '0.4rem' }}>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                disabled={currentPage <= 0 || loading}
                onClick={() =>
                  onChangeFilters({ ...filters, page: Math.max(0, currentPage - 1) })
                }
              >
                ← Prec.
              </button>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                disabled={currentPage + 1 >= totalPages || loading}
                onClick={() => onChangeFilters({ ...filters, page: currentPage + 1 })}
              >
                Succ. →
              </button>
            </div>
          </div>
        )}
      </section>

      {/* Modal Modifica */}
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
