import { resolveCategoryLabel } from '../../../shared/utils/constants';
import { computePercentage, formatCurrency } from '../../../shared/utils/formatters';
import type { CategoryMetadata } from '../../expenses/types/expense.types';

interface CategoryBreakdownProps {
  amountByCategory: Record<string, number>;
  totalAmount: number;
  categories: CategoryMetadata[];
}

export function CategoryBreakdown({
  amountByCategory,
  totalAmount,
  categories,
}: CategoryBreakdownProps) {
  const entries = Object.entries(amountByCategory).sort((a, b) => b[1] - a[1]);

  if (entries.length === 0) {
    return (
      <div className="empty-state">
        <p>Nessun dato per categoria disponibile nel periodo selezionato.</p>
      </div>
    );
  }

  return (
    <div className="category-list">
      {entries.map(([code, amount]) => {
        const pct = computePercentage(amount, totalAmount);
        return (
          <div key={code} className="category-row">
            <div className="category-row-header">
              <span>{resolveCategoryLabel(code, categories)}</span>
              <span>
                <strong>{formatCurrency(amount)}</strong> ({pct}%)
              </span>
            </div>
            <progress className="progress-bar" value={pct} max={100} />
          </div>
        );
      })}
    </div>
  );
}
