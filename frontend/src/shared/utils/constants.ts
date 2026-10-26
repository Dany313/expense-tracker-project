import type { CategoryMetadata, ExpenseCategoryCode } from '../../features/expenses/types/expense.types';

export const DEFAULT_CATEGORIES: CategoryMetadata[] = [
  { code: 'GROCERIES', displayName: 'Alimentari e Supermercato' },
  { code: 'DINING', displayName: 'Ristorazione, Bar e Delivery' },
  { code: 'TRANSPORT', displayName: 'Trasporti, Carburante e Auto' },
  { code: 'HOUSING', displayName: 'Casa, Affitto e Manutenzione' },
  { code: 'BILLS_AND_UTILITIES', displayName: 'Bollette, Utenze e Tasse' },
  { code: 'HEALTH', displayName: 'Salute, Farmacia e Visite Mediche' },
  { code: 'PERSONAL_CARE', displayName: 'Cura della Persona e Igiene Casa' },
  { code: 'SHOPPING', displayName: 'Acquisti, Abbigliamento ed Elettronica' },
  { code: 'ENTERTAINMENT', displayName: 'Svago, Abbonamenti e Tempo Libero' },
  { code: 'TRAVEL', displayName: 'Viaggi e Vacanze' },
  { code: 'EDUCATION', displayName: 'Istruzione, Libri e Formazione' },
  { code: 'OTHER', displayName: 'Altro' },
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
