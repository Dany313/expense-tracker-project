-- ===================================================================
-- Migration V3: Update categories to new user-defined set
-- ===================================================================

UPDATE expenses
SET category = 'SPESA_ALIMENTARE_E_CASA'
WHERE category IN ('GROCERIES', 'PERSONAL_CARE', 'HOUSING', 'OTHER');

UPDATE expenses
SET category = 'SVAGO'
WHERE category IN ('ENTERTAINMENT', 'DINING');

UPDATE expenses
SET category = 'MANUTENZIONE_AUTO_ORDINARIA'
WHERE category = 'TRANSPORT' AND (subcategory ILIKE '%tagliando%' OR store_name ILIKE '%meccanico%');

UPDATE expenses
SET category = 'CARBURANTE_E_TRASPORTI'
WHERE category = 'TRANSPORT';

UPDATE expenses
SET category = 'ABBIGLIAMENTO'
WHERE category = 'SHOPPING' AND (store_name = 'PEPCO' OR subcategory ILIKE '%abbigliamento%');

UPDATE expenses
SET category = 'SPESA_ALIMENTARE_E_CASA'
WHERE category = 'SHOPPING';

UPDATE expenses
SET category = 'BOLLETTE'
WHERE category = 'BILLS_AND_UTILITIES';

UPDATE expenses
SET category = 'SALUTE_E_SPORT'
WHERE category = 'HEALTH';

UPDATE expenses
SET category = 'VIAGGI'
WHERE category = 'TRAVEL';
