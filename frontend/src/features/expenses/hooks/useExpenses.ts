import { useCallback, useEffect, useState } from 'react';
import { ApiError } from '../../../core/api/httpClient';
import { useSplitShared } from '../../../core/context/SplitSharedContext';
import { DEFAULT_CATEGORIES } from '../../../shared/utils/constants';
import { formatCurrency } from '../../../shared/utils/formatters';
import { expensesApi } from '../api/expensesApi';
import type {
  CategoryMetadata,
  ExpenseCreateRequest,
  ExpenseFilterParams,
  ExpenseResponse,
  ExpenseSummaryResponse,
} from '../types/expense.types';

const INITIAL_FILTERS: ExpenseFilterParams = {
  startDate: '',
  endDate: '',
  storeName: '',
  category: '',
  subcategory: '',
  page: 0,
  size: 12,
};

export function useExpenses(enabled: boolean) {
  const { splitShared } = useSplitShared();
  const [categories, setCategories] = useState<CategoryMetadata[]>(DEFAULT_CATEGORIES);
  const [filters, setFilters] = useState<ExpenseFilterParams>(INITIAL_FILTERS);
  const [expenses, setExpenses] = useState<ExpenseResponse[]>([]);
  const [totalElements, setTotalElements] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(0);
  const [summary, setSummary] = useState<ExpenseSummaryResponse | null>(null);

  const [loading, setLoading] = useState<boolean>(false);
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!enabled) return;
    expensesApi
      .getCategories()
      .then((list) => {
        if (list.length > 0) setCategories(list);
      })
      .catch(() => {});
  }, [enabled]);

  const refreshData = useCallback(
    async (activeFilters: ExpenseFilterParams = filters) => {
      if (!enabled) return;
      setLoading(true);
      setErrorMessage(null);
      try {
        const [pageResult, summaryResult] = await Promise.all([
          expensesApi.searchExpenses(activeFilters),
          expensesApi.getSummary(
            activeFilters.startDate || undefined,
            activeFilters.endDate || undefined,
            splitShared
          ),
        ]);
        setExpenses(pageResult.content ?? []);
        setTotalElements(pageResult.page?.totalElements ?? pageResult.content?.length ?? 0);
        setTotalPages(pageResult.page?.totalPages ?? 1);
        setSummary(summaryResult);
      } catch (err) {
        setErrorMessage(
          err instanceof ApiError
            ? err.message
            : 'Impossibile recuperare i dati dall’API Gateway.'
        );
      } finally {
        setLoading(false);
      }
    },
    [enabled, filters, splitShared]
  );

  useEffect(() => {
    if (enabled) {
      void refreshData(filters);
    }
  }, [enabled, filters, splitShared, refreshData]);

  const createExpense = async (payload: ExpenseCreateRequest): Promise<boolean> => {
    setSubmitting(true);
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      const created = await expensesApi.createExpense(payload);
      setSuccessMessage(
        `Spesa registrata: ${created.storeName} (${formatCurrency(created.amount)})`
      );
      await refreshData({ ...filters, page: 0 });
      return true;
    } catch (err) {
      if (err instanceof ApiError && err.fieldErrors) {
        const details = Object.entries(err.fieldErrors)
          .map(([field, msg]) => `${field}: ${msg}`)
          .join(' | ');
        setErrorMessage(`${err.message} (${details})`);
      } else {
        setErrorMessage(
          err instanceof Error ? err.message : 'Errore durante il salvataggio della spesa'
        );
      }
      return false;
    } finally {
      setSubmitting(false);
    }
  };

  const replaceExpense = async (
    id: string,
    payload: ExpenseCreateRequest
  ): Promise<boolean> => {
    setSubmitting(true);
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      const updated = await expensesApi.replaceExpense(id, payload);
      setSuccessMessage(
        `Spesa aggiornata: ${updated.storeName} (${formatCurrency(updated.amount)})`
      );
      await refreshData(filters);
      return true;
    } catch (err) {
      setErrorMessage(
        err instanceof Error ? err.message : 'Errore durante l’aggiornamento della spesa'
      );
      return false;
    } finally {
      setSubmitting(false);
    }
  };

  const deleteExpense = async (expense: ExpenseResponse): Promise<void> => {
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      await expensesApi.deleteExpense(expense.id);
      setSuccessMessage(
        `Spesa eliminata: ${expense.storeName} (${formatCurrency(expense.amount)})`
      );
      await refreshData(filters);
    } catch (err) {
      setErrorMessage(
        err instanceof Error ? err.message : 'Errore durante l’eliminazione della spesa'
      );
    }
  };

  const resetFilters = () => {
    setFilters(INITIAL_FILTERS);
  };

  return {
    categories,
    filters,
    setFilters,
    resetFilters,
    expenses,
    totalElements,
    totalPages,
    summary,
    loading,
    submitting,
    errorMessage,
    successMessage,
    clearMessages: () => {
      setErrorMessage(null);
      setSuccessMessage(null);
    },
    refreshData,
    createExpense,
    replaceExpense,
    deleteExpense,
  };
}
