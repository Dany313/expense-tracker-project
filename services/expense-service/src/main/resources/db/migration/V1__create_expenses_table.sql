CREATE TABLE IF NOT EXISTS expenses (
    id                UUID           PRIMARY KEY,
    expense_date      DATE           NOT NULL,
    store_name        VARCHAR(150)   NOT NULL,
    amount            NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    category          VARCHAR(50)    NOT NULL,
    subcategory       VARCHAR(100),
    description       VARCHAR(500),
    is_recurring      BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version           BIGINT         NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_expenses_expense_date
    ON expenses (expense_date DESC);

CREATE INDEX IF NOT EXISTS idx_expenses_category_subcategory
    ON expenses (category, subcategory);

CREATE INDEX IF NOT EXISTS idx_expenses_store_name
    ON expenses (store_name);
