export type ExpenseCategoryCode =
  | 'GROCERIES'
  | 'DINING'
  | 'TRANSPORT'
  | 'HOUSING'
  | 'BILLS_AND_UTILITIES'
  | 'HEALTH'
  | 'PERSONAL_CARE'
  | 'SHOPPING'
  | 'ENTERTAINMENT'
  | 'TRAVEL'
  | 'EDUCATION'
  | 'OTHER';

export interface CategoryMetadata {
  code: ExpenseCategoryCode;
  displayName: string;
}

export interface ExpenseCreateRequest {
  expenseDate: string;
  storeName: string;
  amount: number;
  category: ExpenseCategoryCode;
  subcategory?: string | null;
  description?: string | null;
  recurring?: boolean;
  shared?: boolean;
}

export interface ExpenseUpdateRequest {
  expenseDate?: string;
  storeName?: string;
  amount?: number;
  category?: ExpenseCategoryCode;
  subcategory?: string | null;
  description?: string | null;
  recurring?: boolean;
  shared?: boolean;
}

export interface ExpenseResponse {
  id: string;
  expenseDate: string;
  storeName: string;
  amount: number;
  category: ExpenseCategoryCode;
  categoryDisplayName: string;
  subcategory: string | null;
  description: string | null;
  recurring: boolean;
  shared: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PagedExpenseResponse {
  content: ExpenseResponse[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}

export interface ExpenseSummaryResponse {
  startDate: string | null;
  endDate: string | null;
  totalAmount: number;
  averageMonthlyAmount: number;
  currentMonthTotalAmount: number;
  amountByCategory: Record<string, number>;
  amountByStore: Record<string, number>;
}

export interface ExpenseFilterParams {
  startDate?: string;
  endDate?: string;
  storeName?: string;
  category?: ExpenseCategoryCode | '';
  subcategory?: string;
  minAmount?: string;
  maxAmount?: string;
  recurring?: string;
  shared?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface ProblemDetailError {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  timestamp?: string;
  errors?: Record<string, string>;
}
