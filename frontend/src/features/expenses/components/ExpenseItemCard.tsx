import { formatCurrency, formatDateIt } from '../../../shared/utils/formatters';
import type { ExpenseResponse } from '../types/expense.types';

interface ExpenseItemCardProps {
  expense: ExpenseResponse;
  effectiveAmount: number;
  splitShared: boolean;
  onEdit: () => void;
  onDelete: () => void;
  onToggleShared: () => void;
}

export function ExpenseItemCard({
  expense,
  effectiveAmount,
  splitShared,
  onEdit,
  onDelete,
  onToggleShared,
}: ExpenseItemCardProps) {
  return (
    <article className="expense-item-card" aria-label={`Spesa da ${expense.storeName}`}>
      <div className="expense-item-top">
        <div>
          <div className="expense-item-store">{expense.storeName}</div>
          <div className="expense-item-date">{formatDateIt(expense.expenseDate)}</div>
        </div>
        <div className="expense-item-amount">
          <div>{formatCurrency(effectiveAmount)}</div>
          {splitShared && expense.shared && (
            <div className="text-muted" style={{ fontSize: '0.72rem', fontWeight: 400 }}>
              tot. {formatCurrency(expense.amount)}
            </div>
          )}
        </div>
      </div>

      <div className="expense-item-details">
        <span className="badge badge-accent">
          {expense.categoryDisplayName || expense.category}
        </span>
        <button
          type="button"
          className={`badge ${expense.shared ? 'badge-shared' : 'badge-ghost'}`}
          title="Tocca per cambiare modalità personale / condivisa"
          onClick={onToggleShared}
        >
          {expense.shared ? '👥 Condivisa (50%)' : '👤 Personale'}
        </button>
        {expense.recurring && (
          <span className="badge" title="Spesa ricorrente">
            🔄 Ricorrente
          </span>
        )}
        {expense.subcategory && (
          <span className="text-muted">· {expense.subcategory}</span>
        )}
        {expense.description && (
          <span className="text-muted">· {expense.description}</span>
        )}
      </div>

      <div className="expense-item-actions">
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          onClick={onEdit}
        >
          Modifica
        </button>
        <button
          type="button"
          className="btn btn-danger btn-sm"
          onClick={onDelete}
        >
          Elimina
        </button>
      </div>
    </article>
  );
}
