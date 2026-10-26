import { httpRequest } from '../../../core/api/httpClient';
import type {
  CategoryMetadata,
  ExpenseCreateRequest,
  ExpenseFilterParams,
  ExpenseResponse,
  ExpenseSummaryResponse,
  ExpenseUpdateRequest,
  PagedExpenseResponse,
} from '../types/expense.types';

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '/api/v1/expenses';

export const expensesApi = {
  async createExpense(payload: ExpenseCreateRequest): Promise<ExpenseResponse> {
    return httpRequest<ExpenseResponse>(API_BASE, {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },

  async searchExpenses(params: ExpenseFilterParams = {}): Promise<PagedExpenseResponse> {
    const query = new URLSearchParams();
    if (params.startDate) query.set('startDate', params.startDate);
    if (params.endDate) query.set('endDate', params.endDate);
    if (params.storeName?.trim()) query.set('storeName', params.storeName.trim());
    if (params.category) query.set('category', params.category);
    if (params.subcategory?.trim()) query.set('subcategory', params.subcategory.trim());
    if (params.minAmount) query.set('minAmount', params.minAmount);
    if (params.maxAmount) query.set('maxAmount', params.maxAmount);
    if (params.recurring !== undefined && params.recurring !== '') {
      query.set('recurring', params.recurring);
    }
    if (params.shared !== undefined && params.shared !== '') {
      query.set('shared', params.shared);
    }
    query.set('page', String(params.page ?? 0));
    query.set('size', String(params.size ?? 12));
    query.set('sort', params.sort ?? 'expenseDate,desc');

    return httpRequest<PagedExpenseResponse>(`${API_BASE}?${query.toString()}`);
  },

  async replaceExpense(id: string, payload: ExpenseCreateRequest): Promise<ExpenseResponse> {
    return httpRequest<ExpenseResponse>(`${API_BASE}/${encodeURIComponent(id)}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    });
  },

  async updateExpense(id: string, payload: ExpenseUpdateRequest): Promise<ExpenseResponse> {
    return httpRequest<ExpenseResponse>(`${API_BASE}/${encodeURIComponent(id)}`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    });
  },

  async deleteExpense(id: string): Promise<void> {
    return httpRequest<void>(`${API_BASE}/${encodeURIComponent(id)}`, {
      method: 'DELETE',
    });
  },

  async getSummary(
    startDate?: string,
    endDate?: string,
    splitShared?: boolean
  ): Promise<ExpenseSummaryResponse> {
    const query = new URLSearchParams();
    if (startDate) query.set('startDate', startDate);
    if (endDate) query.set('endDate', endDate);
    if (splitShared) query.set('splitShared', 'true');
    const suffix = query.toString() ? `?${query.toString()}` : '';
    return httpRequest<ExpenseSummaryResponse>(`${API_BASE}/summary${suffix}`);
  },

  async getCategories(): Promise<CategoryMetadata[]> {
    return httpRequest<CategoryMetadata[]>(`${API_BASE}/categories`);
  },
};
