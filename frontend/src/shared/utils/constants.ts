import type { CategoryMetadata, ExpenseCategoryCode } from '../../features/expenses/types/expense.types';

export const DEFAULT_CATEGORIES: CategoryMetadata[] = [];

export const KNOWN_STORES: string[] = [
  'CONAD',
  'ALDI',
  'TIGOTA',
  'ESSELUNGA',
  'FAMILA',
  'EUROSPIN',
  'HAPPYCASA',
  'COOP',
  'LIDL',
];

export function resolveCategoryLabel(
  code: ExpenseCategoryCode | string,
  categories: CategoryMetadata[] = DEFAULT_CATEGORIES
): string {
  if (!code) return '';
  return categories.find((c) => c.code.toLowerCase() === code.toLowerCase())?.displayName ?? code;
}
