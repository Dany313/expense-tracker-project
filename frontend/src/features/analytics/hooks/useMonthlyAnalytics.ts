import { useCallback, useEffect, useMemo, useState } from 'react';
import { ApiError } from '../../../core/api/httpClient';
import { useSplitShared } from '../../../core/context/SplitSharedContext';
import { computePercentage, formatYearMonthIt } from '../../../shared/utils/formatters';
import { expensesApi } from '../../expenses/api/expensesApi';
import type {
  ExpenseCategoryCode,
  ExpenseResponse,
} from '../../expenses/types/expense.types';

export interface MonthlyBreakdownRow {
  yearMonth: string;
  monthLabel: string;
  totalAmount: number;
  expenseCount: number;
  topCategory: ExpenseCategoryCode | string;
  shareOfPeakPercent: number;
}

export function useMonthlyAnalytics(enabled: boolean) {
  const { splitShared, getEffectiveAmount } = useSplitShared();
  const [selectedCategories, setSelectedCategories] = useState<ExpenseCategoryCode[]>([]);
  const [selectedYear, setSelectedYear] = useState<string>('');
  const [allExpenses, setAllExpenses] = useState<ExpenseResponse[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const loadMonthlyData = useCallback(async () => {
    if (!enabled) return;
    setLoading(true);
    setError(null);
    try {
      const response = await expensesApi.searchExpenses({
        page: 0,
        size: 1000,
        sort: 'expenseDate,desc',
      });
      setAllExpenses(response.content ?? []);
    } catch (err) {
      setError(
        err instanceof ApiError
          ? err.message
          : 'Impossibile caricare i dati mensili delle spese.'
      );
    } finally {
      setLoading(false);
    }
  }, [enabled]);

  useEffect(() => {
    void loadMonthlyData();
  }, [loadMonthlyData]);

  const toggleCategory = useCallback((code: ExpenseCategoryCode) => {
    setSelectedCategories((prev) =>
      prev.includes(code) ? prev.filter((item) => item !== code) : [...prev, code]
    );
  }, []);

  const clearCategories = useCallback(() => {
    setSelectedCategories([]);
  }, []);

  const availableYears = useMemo(() => {
    const years = new Set<string>();
    for (const exp of allExpenses) {
      if (exp.expenseDate && exp.expenseDate.length >= 4) {
        years.add(exp.expenseDate.slice(0, 4));
      }
    }
    return Array.from(years).sort((a, b) => b.localeCompare(a));
  }, [allExpenses]);

  const monthlyRows = useMemo<MonthlyBreakdownRow[]>(() => {
    const filtered = allExpenses.filter((e) => {
      const matchesYear = !selectedYear || e.expenseDate.startsWith(selectedYear);
      const matchesCategory =
        selectedCategories.length === 0 || selectedCategories.includes(e.category);
      return matchesYear && matchesCategory;
    });

    const grouped = new Map<
      string,
      {
        totalAmount: number;
        count: number;
        categorySums: Map<string, number>;
      }
    >();

    for (const exp of filtered) {
      const ym = exp.expenseDate.slice(0, 7);
      const entry = grouped.get(ym) ?? {
        totalAmount: 0,
        count: 0,
        categorySums: new Map<string, number>(),
      };
      const effectiveAmount = getEffectiveAmount(exp.amount, exp.shared);
      entry.totalAmount += effectiveAmount;
      entry.count += 1;
      const catKey = exp.category;
      entry.categorySums.set(
        catKey,
        (entry.categorySums.get(catKey) ?? 0) + effectiveAmount
      );
      grouped.set(ym, entry);
    }

    const sortedMonths = Array.from(grouped.entries()).sort((a, b) =>
      b[0].localeCompare(a[0])
    );

    const peakAmount = sortedMonths.reduce(
      (max, [, val]) => Math.max(max, val.totalAmount),
      0
    );

    return sortedMonths.map(([yearMonth, data]) => {
      let topCategory = 'OTHER';
      let topCatAmount = -1;
      for (const [cat, sum] of data.categorySums.entries()) {
        if (sum > topCatAmount) {
          topCatAmount = sum;
          topCategory = cat;
        }
      }

      return {
        yearMonth,
        monthLabel: formatYearMonthIt(yearMonth),
        totalAmount: Number(data.totalAmount.toFixed(2)),
        expenseCount: data.count,
        topCategory,
        shareOfPeakPercent: computePercentage(data.totalAmount, peakAmount),
      };
    });
  }, [allExpenses, selectedCategories, selectedYear, splitShared, getEffectiveAmount]);

  const totalPeriodAmount = useMemo(
    () => monthlyRows.reduce((acc, row) => acc + row.totalAmount, 0),
    [monthlyRows]
  );

  const averageMonthlyAmount = useMemo(
    () => (monthlyRows.length > 0 ? totalPeriodAmount / monthlyRows.length : 0),
    [monthlyRows.length, totalPeriodAmount]
  );

  const peakMonth = useMemo(() => {
    if (monthlyRows.length === 0) return null;
    return [...monthlyRows].sort((a, b) => b.totalAmount - a.totalAmount)[0];
  }, [monthlyRows]);

  return {
    selectedCategories,
    toggleCategory,
    clearCategories,
    selectedYear,
    setSelectedYear,
    availableYears,
    monthlyRows,
    totalPeriodAmount,
    averageMonthlyAmount,
    peakMonth,
    loading,
    error,
    reload: loadMonthlyData,
  };
}
