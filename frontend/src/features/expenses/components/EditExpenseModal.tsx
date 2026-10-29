import React, { useEffect, useState } from 'react';
import { useDialogModal } from '../../../shared/hooks/useDialogModal';
import { useFormValidity } from '../../../shared/hooks/useFormValidity';
import type {
  CategoryMetadata,
  ExpenseCategoryCode,
  ExpenseCreateRequest,
  ExpenseResponse,
} from '../types/expense.types';

interface EditExpenseModalProps {
  expense: ExpenseResponse | null;
  categories: CategoryMetadata[];
  submitting: boolean;
  onClose: () => void;
  onSave: (id: string, payload: ExpenseCreateRequest) => Promise<boolean>;
}

export function EditExpenseModal({
  expense,
  categories,
  submitting,
  onClose,
  onSave,
}: EditExpenseModalProps) {
  const dialogRef = useDialogModal(Boolean(expense), onClose);
  const formRef = useFormValidity<HTMLFormElement>(expense);

  const [expenseDate, setExpenseDate] = useState<string>('');
  const [storeName, setStoreName] = useState<string>('');
  const [amount, setAmount] = useState<string>('');
  const [category, setCategory] = useState<string>('');
  const [subcategory, setSubcategory] = useState<string>('');
  const [description, setDescription] = useState<string>('');
  const [recurring, setRecurring] = useState<boolean>(false);
  const [shared, setShared] = useState<boolean>(false);

  useEffect(() => {
    if (!expense) return;
    setExpenseDate(expense.expenseDate);
    setStoreName(expense.storeName);
    setAmount(String(expense.amount));
    setCategory(expense.category);
    setSubcategory(expense.subcategory ?? '');
    setDescription(expense.description ?? '');
    setRecurring(expense.recurring);
    setShared(expense.shared);
  }, [expense]);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!expense) return;
    const ok = await onSave(expense.id, {
      expenseDate,
      storeName: storeName.trim(),
      amount: Number(amount),
      category,
      subcategory: subcategory.trim() ? subcategory.trim() : null,
      description: description.trim() ? description.trim() : null,
      recurring,
      shared,
    });
    if (ok) {
      onClose();
    }
  };

  return (
    <dialog
      ref={dialogRef}
      closedby="any"
      className="modal-dialog"
      aria-labelledby="edit-expense-modal-title"
    >
      {expense && (
        <form ref={formRef} className="form-stack" onSubmit={handleSubmit}>
          <div className="surface-card-header">
            <h2 id="edit-expense-modal-title" className="surface-card-title">
              Modifica Spesa
            </h2>
            <button type="button" className="btn btn-ghost btn-sm" onClick={onClose}>
              Chiudi
            </button>
          </div>

          <div className="form-grid-2">
            <div className="field-group">
              <label htmlFor="edit-date" className="field-label">
                Data *
              </label>
              <input
                id="edit-date"
                type="date"
                className="input-control"
                required
                value={expenseDate}
                onChange={(e) => setExpenseDate(e.target.value)}
              />
            </div>

            <div className="field-group">
              <label htmlFor="edit-amount" className="field-label">
                Importo (€) *
              </label>
              <input
                id="edit-amount"
                type="number"
                step="0.01"
                min="0.01"
                className="input-control"
                required
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
              />
            </div>
          </div>

          <div className="field-group">
            <label htmlFor="edit-store" className="field-label">
              Negozio / Esercente *
            </label>
            <input
              id="edit-store"
              type="text"
              className="input-control"
              required
              value={storeName}
              onChange={(e) => setStoreName(e.target.value)}
            />
          </div>

          <div className="form-grid-2">
            <div className="field-group">
              <label htmlFor="edit-category" className="field-label">
                Categoria *
              </label>
              <select
                id="edit-category"
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

            <div className="field-group">
              <label htmlFor="edit-subcategory" className="field-label">
                Sottocategoria
              </label>
              <input
                id="edit-subcategory"
                type="text"
                className="input-control"
                value={subcategory}
                onChange={(e) => setSubcategory(e.target.value)}
              />
            </div>
          </div>

          <div className="field-group">
            <label htmlFor="edit-description" className="field-label">
              Note / Descrizione
            </label>
            <textarea
              id="edit-description"
              rows={2}
              className="input-control"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>

          <div className="form-grid-2">
            <label className="checkbox-card" htmlFor="edit-recurring">
              <input
                id="edit-recurring"
                type="checkbox"
                checked={recurring}
                onChange={(e) => setRecurring(e.target.checked)}
              />
              <span>Spesa periodica / ricorrente</span>
            </label>

            <label className="checkbox-card" htmlFor="edit-shared">
              <input
                id="edit-shared"
                type="checkbox"
                checked={shared}
                onChange={(e) => setShared(e.target.checked)}
              />
              <span>👥 Spesa condivisa (50%)</span>
            </label>
          </div>

          <div className="row-actions">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              Annulla
            </button>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Salvataggio...' : 'Salva Modifiche'}
            </button>
          </div>
        </form>
      )}
    </dialog>
  );
}
