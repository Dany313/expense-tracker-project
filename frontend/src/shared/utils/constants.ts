import type { CategoryMetadata, ExpenseCategoryCode } from '../../features/expenses/types/expense.types';

export const DEFAULT_CATEGORIES: CategoryMetadata[] = [
  { code: 'SPESA_ALIMENTARE_E_CASA', displayName: 'Spesa alimentare e casa' },
  { code: 'AFFITTO', displayName: 'Affitto' },
  { code: 'BOLLETTE', displayName: 'Bollette' },
  { code: 'CARBURANTE_E_TRASPORTI', displayName: 'Carburante e trasporti' },
  { code: 'MANUTENZIONE_AUTO_ORDINARIA', displayName: 'Manutenzione auto ordinaria' },
  { code: 'MANUTENZIONE_AUTO_STRAORDINARIA', displayName: 'Manutenzione auto straordinaria' },
  { code: 'SVAGO', displayName: 'Svago' },
  { code: 'USCITE_FUORI', displayName: 'Uscite fuori' },
  { code: 'SALUTE_E_SPORT', displayName: 'Salute e sport' },
  { code: 'ABBIGLIAMENTO', displayName: 'Abbigliamento' },
  { code: 'VIAGGI', displayName: 'Viaggi' },
];

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
  return categories.find((c) => c.code === code)?.displayName ?? code;
}
