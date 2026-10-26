# Guida Inizializzazione Database (Modalità A: On-Demand Seed)

Questa guida descrive come inizializzare il database PostgreSQL (locale o su **Neon.tech**) con i dati storici delle spese in modo controllato e on-demand, senza che Flyway pre-popoli automaticamente il database all'avvio dell'applicazione.

---

## 1. Architettura delle Migrazioni Flyway

Per garantire che in produzione su Neon il database parta pulito (0 record), lo schema DDL è stato separato dai dati di seed:

* **`services/expense-service/src/main/resources/db/migration/`**:
  * `V1__create_expenses_table.sql`: Crea la tabella `expenses` e i relativi indici.
  * `V2__add_is_shared_column.sql`: Aggiunge la colonna `is_shared` e l'indice per le spese condivise.
* **`scripts/db/initial_expenses_seed.sql`**:
  * Contiene l'inserimento atomico e idempotente (`ON CONFLICT DO NOTHING`) delle 95 spese storiche ricavate dal foglio Excel.

---

## 2. Come Eseguire il Seed On-Demand

Lo script `scripts/seed-database.sh` automatizza l'importazione supportando diverse modalità.

### A. Su Neon.tech (Produzione o Staging)

Quando crei il tuo database su Neon, ottieni la connection string (es. `postgresql://user:pass@ep-xyz.neon.tech/expense_tracker?sslmode=require`).

Puoi eseguire il seed semplicemente passando l'URL allo script:
```bash
./scripts/seed-database.sh "postgresql://user:pass@ep-xyz.neon.tech/expense_tracker?sslmode=require"
```
Oppure definendo la variabile d'ambiente:
```bash
DATABASE_URL="postgresql://user:pass@ep-xyz.neon.tech/expense_tracker?sslmode=require" ./scripts/seed-database.sh -y
```

### B. Tramite Neon Web Console (Alternativa Web)

Se non hai un client `psql` sottomano:
1. Accedi alla dashboard di **Neon.tech**.
2. Apri il progetto e clicca su **SQL Editor**.
3. Apri il file [initial_expenses_seed.sql](file:///Users/daniele/Dev/expense-tracker-project/scripts/db/initial_expenses_seed.sql), copia il contenuto e incollalo nell'editor.
4. Clicca su **Run**.

### C. Sul Database Locale Docker

Per inizializzare il database Docker locale:
```bash
./scripts/seed-database.sh --local -y
```

---

## 3. Idempotenza e Sicurezza

* Lo script utilizza una transazione (`BEGIN ... COMMIT`) per garantire che i dati vengano inseriti solo se l'intera operazione ha successo.
* La clausola `ON CONFLICT (id) DO NOTHING;` impedisce qualsiasi duplicazione se lo script viene rilanciato per errore una seconda volta.
