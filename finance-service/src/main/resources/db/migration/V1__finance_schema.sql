CREATE TABLE profiles (
    user_id                      UUID PRIMARY KEY,
    tax_regime                   VARCHAR(3)    NOT NULL DEFAULT 'NEW',
    monthly_fixed_deductions     NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (monthly_fixed_deductions >= 0),
    annual_old_regime_deductions NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (annual_old_regime_deductions >= 0),
    updated_at                   TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE incomes (
    id         UUID PRIMARY KEY,
    user_id    UUID          NOT NULL,
    type       VARCHAR(20)   NOT NULL,
    label      VARCHAR(100)  NOT NULL,
    amount     NUMERIC(15,2) NOT NULL CHECK (amount >= 0),
    frequency  VARCHAR(10)   NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_incomes_user ON incomes (user_id);

CREATE TABLE expenses (
    id             UUID PRIMARY KEY,
    user_id        UUID          NOT NULL,
    category       VARCHAR(20)   NOT NULL,
    kind           VARCHAR(10)   NOT NULL,
    label          VARCHAR(100)  NOT NULL,
    monthly_amount NUMERIC(15,2) NOT NULL CHECK (monthly_amount >= 0),
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_expenses_user ON expenses (user_id);

CREATE TABLE debts (
    id                UUID PRIMARY KEY,
    user_id           UUID          NOT NULL,
    loan_type         VARCHAR(20)   NOT NULL,
    lender            VARCHAR(100),
    principal         NUMERIC(15,2) NOT NULL CHECK (principal > 0),
    annual_rate       NUMERIC(5,2)  NOT NULL CHECK (annual_rate >= 0),
    tenure_months     INTEGER       NOT NULL CHECK (tenure_months > 0),
    emi               NUMERIC(15,2) NOT NULL CHECK (emi >= 0),
    remaining_balance NUMERIC(15,2) NOT NULL CHECK (remaining_balance >= 0),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_debts_user ON debts (user_id);

CREATE TABLE goals (
    id            UUID PRIMARY KEY,
    user_id       UUID          NOT NULL,
    name          VARCHAR(100)  NOT NULL,
    target_amount NUMERIC(15,2) NOT NULL CHECK (target_amount > 0),
    saved_amount  NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (saved_amount >= 0),
    target_date   DATE          NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_goals_user ON goals (user_id);

-- Transactional outbox: rows are written in the same transaction as the business change,
-- then relayed to Kafka by OutboxPublisher.
CREATE TABLE outbox_events (
    id             UUID PRIMARY KEY,
    aggregate_type VARCHAR(30) NOT NULL,
    aggregate_id   UUID,
    event_type     VARCHAR(50) NOT NULL,
    user_id        UUID        NOT NULL,
    payload        TEXT        NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ
);
CREATE INDEX idx_outbox_unpublished ON outbox_events (created_at) WHERE published_at IS NULL;
