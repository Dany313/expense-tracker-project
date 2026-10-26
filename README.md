# Expense Tracker Platform (Monorepo Java 25 LTS + Spring Boot 4.1 + Spring AI 2.0)

Architettura **Monorepo Maven Multi-Module** per la gestione intelligente delle spese tramite microservizi **Spring Boot 4.1.1**, **PostgreSQL 17**, **Spring AI (Google GenAI / Gemini)** e **MCP (Model Context Protocol)**.

---

## Struttura del Monorepo

```text
expense-tracker-project/
├── pom.xml                           # Parent & Aggregator POM (Java 25, Spring Boot 4.1.1, Spring AI 2.0.1)
├── compose.yaml                      # Infrastruttura locale (PostgreSQL 17)
└── services/
    ├── expense-service/              # Microservizio principale CRUD spese su PostgreSQL (REST + MCP Server, porta 8080)
    ├── receipt-ocr-service/          # Microservizio AI OCR scontrini tramite Vision AI (REST + MCP Server, porta 8081)
    └── expense-agent-service/        # Microservizio Agente AI (Testo/Vocale -> Gemini + MCP Client -> expense-service, porta 8082)
```

---

## 1. `services/expense-service` (Porta `8080`)

Microservizio principale che gestisce la persistenza delle spese su **PostgreSQL** con migrazioni **Flyway** (`V1__create_expenses_table.sql`).

### API REST (`/api/v1/expenses`)
- `POST /api/v1/expenses` — Crea una nuova spesa
- `GET /api/v1/expenses` — Ricerca paginata e filtrata (`startDate`, `endDate`, `storeName`, `category`, `subcategory`, `paymentMethod`, `minAmount`, `maxAmount`, `recurring`)
- `GET /api/v1/expenses/{id}` — Dettaglio spesa per `UUID`
- `PUT /api/v1/expenses/{id}` — Sostituzione completa
- `PATCH /api/v1/expenses/{id}` — Modifica parziale
- `DELETE /api/v1/expenses/{id}` — Eliminazione spesa
- `GET /api/v1/expenses/summary` — Riepilogo statistico (totale, media, aggregazioni per categoria e negozio)
- `GET /api/v1/expenses/categories` e `GET /api/v1/expenses/payment-methods`

### Tool MCP esposti (`expense-tracker-mcp-server`, endpoint `/sse`)
- `create_expense`, `get_expense_by_id`, `search_expenses`, `update_expense`, `delete_expense`, `get_expense_summary`, `list_expense_metadata`

---

## 2. `services/receipt-ocr-service` (Porta `8081`)

Microservizio AI che riceve la **foto di uno scontrino** e, tramite **Spring AI Multimodal Vision** + **Structured Output**, estrae data, negozio normalizzato e dati della spesa in un JSON già compatibile con `expense-service`.

### API REST (`/api/v1/ocr/receipts`)
- `POST /api/v1/ocr/receipts` (`multipart/form-data`, campo `file`) — Carica il file immagine dello scontrino (`JPEG`, `PNG`, `WEBP`, `HEIC`)
- `POST /api/v1/ocr/receipts/base64` (`application/json`) — Invia l'immagine dello scontrino codificata in Base64

### Tool MCP esposti (`receipt-ocr-mcp-server`, endpoint `/sse`)
- `extract_expense_from_receipt_base64`, `extract_expense_from_receipt_file_path`

---

## 3. `services/expense-agent-service` (Porta `8082`)

Microservizio Agente AI pensato per essere chiamato da un **Bot Telegram**, da **MacroDroid** o da un **Frontend**:
1. Accetta in input **un testo** oppure **un messaggio vocale/audio** (`.ogg` di Telegram, `.mp3`, `.wav`, `.m4a`, `.webm`, `.aac`).
2. Se riceve un vocale, lo **traduce/trascrive in testo** tramite `VoiceTranscriptionService`.
3. Invia il testo a **Gemini (`SpringAiExpenseAgentGateway`)**, che è collegato come **MCP Client (`spring-ai-starter-mcp-client`)** al server MCP di `expense-service` (`http://localhost:8080/sse`).
4. Gemini invoca autonomamente i tool MCP sul microservizio principale (`create_expense`, `search_expenses`, `get_expense_summary`, ecc.) e restituisce la risposta naturale insieme al testo trascritto (`AgentInteractionResponse`).

### API REST (`/api/v1/agent`)
- `POST /api/v1/agent/text` (`application/json`) — Invia un messaggio di testo all'agente
- `POST /api/v1/agent/voice` (`multipart/form-data`, campo `file`) — Invia un file vocale/audio (es. nota vocale Telegram `.ogg`)
- `POST /api/v1/agent/voice/base64` (`application/json`) — Invia un vocale codificato in Base64

---

## Comandi Utili

### Eseguire tutti i test del Monorepo
```bash
./mvnw test
```

### Avviare i microservizi
```bash
# 1. Backend principale con PostgreSQL (porta 8080)
./mvnw spring-boot:run -pl services/expense-service

# 2. Servizio OCR Scontrini (porta 8081)
GEMINI_API_KEY="la_tua_api_key" ./mvnw spring-boot:run -pl services/receipt-ocr-service

# 3. Agente Testo/Vocale collegato via MCP a expense-service (porta 8082)
GEMINI_API_KEY="la_tua_api_key" ./mvnw spring-boot:run -pl services/expense-agent-service
```
