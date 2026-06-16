-- ============================================================
-- Account Aggregator - Initial Schema
-- V1__initial_schema.sql
-- ============================================================

-- Customers
CREATE TABLE customers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id     VARCHAR(100) NOT NULL UNIQUE,   -- FIU's own customer identifier
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255),
    mobile          VARCHAR(15) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_customers_mobile ON customers(mobile);
CREATE INDEX idx_customers_external_id ON customers(external_id);

-- Consent Requests
CREATE TABLE consent_requests (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id         UUID NOT NULL REFERENCES customers(id),
    digio_consent_id    VARCHAR(200) UNIQUE,          -- Digio's consent handle
    digio_doc_id        VARCHAR(200),                 -- Digio document/signing ID
    template_id         VARCHAR(200) NOT NULL,
    status              VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    -- PENDING | ACTIVE | PAUSED | REVOKED | EXPIRED | REJECTED
    purpose_code        VARCHAR(50) NOT NULL,
    purpose_text        TEXT,
    fetch_type          VARCHAR(20) NOT NULL DEFAULT 'ONETIME',  -- ONETIME | PERIODIC
    frequency_unit      VARCHAR(20),
    frequency_value     INT,
    consent_start       TIMESTAMPTZ,
    consent_expiry      TIMESTAMPTZ,
    fi_types            TEXT[],                        -- DEPOSIT, MUTUAL_FUNDS, INSURANCE, etc.
    idempotency_key     VARCHAR(200) UNIQUE NOT NULL,  -- caller-supplied idempotency
    redirect_url        VARCHAR(500),
    callback_url        VARCHAR(500),
    raw_request         JSONB,
    raw_response        JSONB,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_consent_customer ON consent_requests(customer_id);
CREATE INDEX idx_consent_status ON consent_requests(status);
CREATE INDEX idx_consent_digio_id ON consent_requests(digio_consent_id);
CREATE INDEX idx_consent_idempotency ON consent_requests(idempotency_key);

-- FI Data Fetch Sessions
CREATE TABLE fi_fetch_sessions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consent_request_id  UUID NOT NULL REFERENCES consent_requests(id),
    digio_session_id    VARCHAR(200) UNIQUE,
    status              VARCHAR(50) NOT NULL DEFAULT 'INITIATED',
    -- INITIATED | PROCESSING | COMPLETED | FAILED | PARTIAL
    fi_types            TEXT[],
    date_range_from     DATE,
    date_range_to       DATE,
    idempotency_key     VARCHAR(200) UNIQUE NOT NULL,
    error_code          VARCHAR(100),
    error_message       TEXT,
    raw_request         JSONB,
    raw_response        JSONB,
    fetched_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_fetch_consent ON fi_fetch_sessions(consent_request_id);
CREATE INDEX idx_fetch_status ON fi_fetch_sessions(status);

-- Normalized Accounts
CREATE TABLE fi_accounts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id      UUID NOT NULL REFERENCES fi_fetch_sessions(id),
    fip_id          VARCHAR(200),
    account_ref     VARCHAR(200),                 -- masked / opaque account ref
    account_type    VARCHAR(50),                  -- SAVINGS, CURRENT, FD, RD, etc.
    fi_type         VARCHAR(50),                  -- DEPOSIT, MUTUAL_FUNDS, etc.
    maskedAccNo     VARCHAR(50),
    currency        VARCHAR(10) DEFAULT 'INR',
    holder_name     VARCHAR(255),
    ifsc_code       VARCHAR(20),
    branch          VARCHAR(255),
    balance         NUMERIC(18,2),
    as_of_date      DATE,
    raw_payload     JSONB,                        -- full FIP response, encrypted at rest
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_accounts_session ON fi_accounts(session_id);

-- Normalized Transactions
CREATE TABLE fi_transactions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id      UUID NOT NULL REFERENCES fi_accounts(id),
    txn_id          VARCHAR(200),
    txn_date        DATE,
    amount          NUMERIC(18,2),
    txn_type        VARCHAR(20),                  -- CREDIT | DEBIT
    mode            VARCHAR(50),                  -- NEFT, IMPS, UPI, etc.
    narration       TEXT,
    reference       VARCHAR(200),
    balance         NUMERIC(18,2),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_txn_account ON fi_transactions(account_id);
CREATE INDEX idx_txn_date ON fi_transactions(txn_date);

-- Audit Log (append-only, never delete)
CREATE TABLE audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    entity_type     VARCHAR(100) NOT NULL,         -- CONSENT | FETCH | ACCOUNT | SYSTEM
    entity_id       UUID,
    action          VARCHAR(100) NOT NULL,
    actor           VARCHAR(200),                  -- user id or system component
    ip_address      INET,
    old_value       JSONB,
    new_value       JSONB,
    metadata        JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_created ON audit_logs(created_at);

-- Webhook Notifications received from Digio
CREATE TABLE webhook_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type      VARCHAR(100),
    digio_entity_id VARCHAR(200),
    payload         JSONB NOT NULL,
    processed       BOOLEAN NOT NULL DEFAULT false,
    processed_at    TIMESTAMPTZ,
    error           TEXT,
    received_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_webhook_processed ON webhook_events(processed);
CREATE INDEX idx_webhook_digio_entity ON webhook_events(digio_entity_id);

-- Triggers for updated_at
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_customers_updated BEFORE UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_consent_updated BEFORE UPDATE ON consent_requests
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_fetch_updated BEFORE UPDATE ON fi_fetch_sessions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
