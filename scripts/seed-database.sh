#!/usr/bin/env bash
# ==============================================================================
# Script di Inizializzazione Database (Modalità A - On Demand Seed)
# Importa lo storico iniziale di 95 spese da 'initial_expenses_seed.sql'.
#
# Utilizzo:
#   ./scripts/seed-database.sh "postgresql://user:pass@ep-xyz.neon.tech/expense_tracker?sslmode=require"
#   ./scripts/seed-database.sh --local
#   DATABASE_URL="..." ./scripts/seed-database.sh -y
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SQL_FILE="${SCRIPT_DIR}/db/initial_expenses_seed.sql"

if [[ ! -f "$SQL_FILE" ]]; then
  SQL_FILE="${PROJECT_ROOT}/services/expense-service/src/main/resources/db/seed/initial_expenses_seed.sql"
fi

if [[ ! -f "$SQL_FILE" ]]; then
  echo "❌ Errore: File di seed non trovato in ${SQL_FILE}" >&2
  exit 1
fi

AUTO_CONFIRM=false
TARGET_URL=""

# Parsing argomenti
while [[ $# -gt 0 ]]; do
  case "$1" in
    -y|--yes)
      AUTO_CONFIRM=true
      shift
      ;;
    --local)
      TARGET_URL="postgresql://expense_user:expense_pass@localhost:5432/expense_tracker"
      shift
      ;;
    postgres://*|postgresql://*)
      TARGET_URL="$1"
      shift
      ;;
    *)
      echo "⚠️ Parametro sconosciuto o non riconosciuto: $1" >&2
      shift
      ;;
  esac
done

# Se non passato come argomento, cerca in environment variable o in .env
if [[ -z "$TARGET_URL" ]]; then
  if [[ -n "${DATABASE_URL:-}" ]]; then
    TARGET_URL="$DATABASE_URL"
  elif [[ -f "${PROJECT_ROOT}/.env" ]]; then
    ENV_URL=$(grep -E '^DATABASE_URL=' "${PROJECT_ROOT}/.env" | cut -d '=' -f2- || true)
    if [[ -n "$ENV_URL" ]]; then
      TARGET_URL="$ENV_URL"
    fi
  fi
fi

if [[ -z "$TARGET_URL" ]]; then
  echo "ℹ️ Nessun DATABASE_URL specificato. Uso come default il database locale:"
  echo "   postgresql://expense_user:expense_pass@localhost:5432/expense_tracker"
  TARGET_URL="postgresql://expense_user:expense_pass@localhost:5432/expense_tracker"
fi

# Maschera la password per il log a video
MASKED_URL=$(echo "$TARGET_URL" | sed -E 's/(:\/\/.*:)(.*)(@)/\1****\3/')
echo "=========================================================="
echo "🌱 Expense Tracker - Inizializzazione Seed (Modalità A)"
echo "🎯 Database Target: ${MASKED_URL}"
echo "📄 File SQL Seed:   ${SQL_FILE}"
echo "=========================================================="

# Conferma utente
if [[ "$AUTO_CONFIRM" != true ]]; then
  read -r -p "Procedere con l'inserimento dei dati iniziali? [y/N] " response
  case "$response" in
    [yY][eE][sS]|[yY])
      ;;
    *)
      echo "Operazione annullata."
      exit 0
      ;;
  esac
fi

# Verifica ed esecuzione
if command -v psql &> /dev/null; then
  echo "🚀 Esecuzione seed tramite client psql locale..."
  psql "$TARGET_URL" -f "$SQL_FILE"
elif command -v docker &> /dev/null && docker ps -q -f name=expense-tracker-postgres &> /dev/null && [[ "$TARGET_URL" == *"localhost:5432"* ]]; then
  echo "🚀 Esecuzione seed tramite container Docker expense-tracker-postgres..."
  docker exec -i expense-tracker-postgres psql -U expense_user -d expense_tracker < "$SQL_FILE"
elif command -v docker &> /dev/null; then
  echo "🚀 Esecuzione seed tramite container Docker temporaneo postgres:18-alpine..."
  docker run --rm -i postgres:18-alpine psql "$TARGET_URL" < "$SQL_FILE"
else
  echo "❌ Client 'psql' non trovato sul sistema locale."
  echo "Puoi eseguire il seed manualmente con uno dei seguenti metodi:"
  echo "  1. Tramite Neon CLI: neon sql \"\$(cat ${SQL_FILE})\""
  echo "  2. Copiando il contenuto di ${SQL_FILE} nell'Editor SQL di Neon Web Console"
  echo "  3. Tramite Docker: docker run --rm -i postgres:17-alpine psql \"${TARGET_URL}\" < \"${SQL_FILE}\""
  exit 1
fi

echo ""
echo "✅ Inizializzazione completata con successo!"
echo "I dati storici sono stati caricati nel database."
