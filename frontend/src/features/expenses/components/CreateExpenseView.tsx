import React, { useEffect, useState } from 'react';
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
  const [category, setCategory] = useState<string>('');
  const [subcategory, setSubcategory] = useState<string>('');
  const [description, setDescription] = useState<string>('');
  const [recurring, setRecurring] = useState<boolean>(false);
  const [shared, setShared] = useState<boolean>(false);

  useEffect(() => {
    if (!category && categories.length > 0) {
      setCategory(categories[0].code);
    }
  }, [categories, category]);

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
    <div style={{ maxWidth: '42rem', marginInline: 'auto', width: '100%' }}>
      <div className="page-header-block" style={{ marginBottom: '1.25rem' }}>
        <div>
          <h1 className="page-title">Nuova Spesa</h1>
          <p className="page-description">
            Inserisci i dati del movimento contabile da salvare
          </p>
        </div>
      </div>

      <section className="surface-card" aria-labelledby="new-expense-card-title">
        <h2 id="new-expense-card-title" className="surface-card-title">
          Dettagli Spesa
        </h2>

        <form ref={formRef} className="form-stack" onSubmit={handleSubmit}>
          {/* Data e Importo */}
          <div className="form-grid-2">
            <div className="field-group">
              <label htmlFor="create-date" className="field-label">
                Data *
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
                placeholder="es. 42.50"
                className="input-control font-mono"
                required
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
              />
            </div>
          </div>

          {/* Negozio ed Esercente */}
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
                placeholder="es. CONAD, TIGOTÀ, ENI..."
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

          {/* Sottocategoria e Note */}
          <div className="form-grid-2">
            <div className="field-group">
              <label htmlFor="create-subcategory" className="field-label">
                Sottocategoria (Opzionale)
              </label>
              <input
                id="create-subcategory"
                name="subcategory"
                type="text"
                placeholder="es. Spesa settimanale, Pranzo..."
                className="input-control"
                value={subcategory}
                onChange={(e) => setSubcategory(e.target.value)}
              />
            </div>

            <div className="field-group">
              <label htmlFor="create-description" className="field-label">
                Note o promemoria (Opzionale)
              </label>
              <input
                id="create-description"
                name="description"
                type="text"
                placeholder="Dettagli aggiuntivi..."
                className="input-control"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>
          </div>

          {/* Opzioni Condivisa e Ricorrente */}
          <div className="form-grid-2">
            <label className="checkbox-card" htmlFor="create-shared">
              <input
                id="create-shared"
                type="checkbox"
                checked={shared}
                onChange={(e) => setShared(e.target.checked)}
              />
              <span>👥 Spesa condivisa (calcola quota al 50%)</span>
            </label>

            <label className="checkbox-card" htmlFor="create-recurring">
              <input
                id="create-recurring"
                type="checkbox"
                checked={recurring}
                onChange={(e) => setRecurring(e.target.checked)}
              />
              <span>🔄 Spesa ricorrente / periodica</span>
            </label>
          </div>

          {/* Azioni in fondo: su mobile si impilano con il salvataggio in cima */}
          <div
            style={{
              display: 'flex',
              gap: '0.75rem',
              justifyContent: 'flex-end',
              marginTop: '0.5rem',
              flexDirection: 'row',
            }}
          >
            <button
              type="button"
              className="btn btn-secondary"
              onClick={onSuccessNavigateArchive}
            >
              Annulla
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={submitting}
            >
              {submitting ? 'Salvataggio...' : 'Salva Movimento'}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}
