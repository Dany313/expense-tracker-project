import React, { useState } from 'react';
import { useFormValidity } from '../../../shared/hooks/useFormValidity';
import { KNOWN_STORES } from '../../../shared/utils/constants';
import { todayIsoDate } from '../../../shared/utils/formatters';
import type {
  CategoryMetadata,
  ExpenseCategoryCode,
  ExpenseCreateRequest,
} from '../types/expense.types';

interface CreateExpenseViewProps {
  categories: CategoryMetadata[];
  submitting: boolean;
  onCreateExpense: (payload: ExpenseCreateRequest) => Promise<boolean>;
  onSuccessNavigateArchive: () => void;
}

export function CreateExpenseView({
  categories,
  submitting,
  onCreateExpense,
  onSuccessNavigateArchive,
}: CreateExpenseViewProps) {
  const formRef = useFormValidity<HTMLFormElement>();

  const [expenseDate, setExpenseDate] = useState<string>(todayIsoDate());
  const [storeName, setStoreName] = useState<string>('');
  const [amount, setAmount] = useState<string>('');
  const [category, setCategory] = useState<ExpenseCategoryCode>('GROCERIES');
  const [subcategory, setSubcategory] = useState<string>('');
  const [description, setDescription] = useState<string>('');
  const [recurring, setRecurring] = useState<boolean>(false);
  const [shared, setShared] = useState<boolean>(false);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const numericAmount = Number(amount);
    if (!Number.isFinite(numericAmount) || numericAmount <= 0) return;

    const ok = await onCreateExpense({
      expenseDate,
      storeName: storeName.trim(),
      amount: numericAmount,
      category,
      subcategory: subcategory.trim() ? subcategory.trim() : null,
      description: description.trim() ? description.trim() : null,
      recurring,
      shared,
    });

    if (ok) {
      setStoreName('');
      setAmount('');
      setSubcategory('');
      setDescription('');
      setRecurring(false);
      setShared(false);
      formRef.current?.reset();
      onSuccessNavigateArchive();
    }
  };

  return (
    <>
      <div className="page-header-block">
        <div>
          <h1 className="page-title">Nuova Spesa</h1>
          <p className="page-description">
            Registra manualmente un nuovo movimento contabile
          </p>
        </div>
      </div>

      <section className="surface-card" aria-labelledby="new-expense-form-title">
        <h2 id="new-expense-form-title" className="surface-card-title">
          Dettagli Movimento
        </h2>

        <form ref={formRef} className="form-stack" onSubmit={handleSubmit}>
          <div className="form-grid-2">
            <div className="field-group">
              <label htmlFor="create-date" className="field-label">
                Data della spesa *
              </label>
              <input
                id="create-date"
                name="expenseDate"
                type="date"
                className="input-control"
                required
                value={expenseDate}
                onChange={(e) => setExpenseDate(e.target.value)}
              />
            </div>

            <div className="field-group">
              <label htmlFor="create-amount" className="field-label">
                Importo (€) *
              </label>
              <input
                id="create-amount"
                name="amount"
                type="number"
                step="0.01"
                min="0.01"
                inputMode="decimal"
                placeholder="es. 38.50"
                className="input-control"
                required
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
              />
            </div>
          </div>

          <div className="form-grid-2">
            <div className="field-group">
              <label htmlFor="create-store" className="field-label">
                Negozio / Esercente *
              </label>
              <input
                id="create-store"
                name="storeName"
                type="text"
                list="known-stores-list"
                maxLength={150}
                placeholder="es. CONAD, ALDI, TIGOTA..."
                className="input-control"
                required
                value={storeName}
                onChange={(e) => setStoreName(e.target.value)}
              />
              <datalist id="known-stores-list">
                {KNOWN_STORES.map((store) => (
                  <option key={store} value={store} />
                ))}
              </datalist>
            </div>

            <div className="field-group">
              <label htmlFor="create-category" className="field-label">
                Categoria *
              </label>
              <select
                id="create-category"
                name="category"
                className="input-control"
                required
                value={category}
                onChange={(e) => setCategory(e.target.value as ExpenseCategoryCode)}
              >
                {categories.map((cat) => (
                  <option key={cat.code} value={cat.code}>
                    {cat.displayName}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="form-grid-2">
            <div className="field-group">
              <label htmlFor="create-subcategory" className="field-label">
                Sottocategoria (Opzionale)
              </label>
              <input
                id="create-subcategory"
                name="subcategory"
                type="text"
                placeholder="es. Spesa settimanale, Farmacia..."
                className="input-control"
                value={subcategory}
                onChange={(e) => setSubcategory(e.target.value)}
              />
            </div>

            <div className="field-group">
              <label htmlFor="create-description" className="field-label">
                Note aggiuntive (Opzionale)
              </label>
              <input
                id="create-description"
                name="description"
                type="text"
                placeholder="Dettagli o promemoria..."
                className="input-control"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>
          </div>

          <div className="form-grid-2">
            <label className="checkbox-card" htmlFor="create-recurring">
              <input
                id="create-recurring"
                type="checkbox"
                checked={recurring}
                onChange={(e) => setRecurring(e.target.checked)}
              />
              <span>Spesa periodica / ricorrente</span>
            </label>

            <label className="checkbox-card" htmlFor="create-shared">
              <input
                id="create-shared"
                type="checkbox"
                checked={shared}
                onChange={(e) => setShared(e.target.checked)}
              />
              <span>👥 Spesa condivisa (divisa al 50%)</span>
            </label>
          </div>

          <div className="row-actions">
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Registrazione in corso...' : 'Salva Spesa'}
            </button>
          </div>
        </form>
      </section>
    </>
  );
}
