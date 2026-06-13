-- ============================================================
-- Schema: AI Chatbot — Jargon Dictionary + Read-Only User
-- Run as superuser / DB owner
-- ============================================================

-- Jargon dictionary: maps short forms → human language
CREATE TABLE IF NOT EXISTS jargon_dictionary (
    id              SERIAL PRIMARY KEY,
    short_form      VARCHAR(50)  NOT NULL,
    full_form       VARCHAR(200) NOT NULL,
    description     TEXT,
    domain          VARCHAR(100),
    example_usage   TEXT,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW(),
    CONSTRAINT uq_jargon_short_form UNIQUE (short_form)
);

-- Generic entities (replace with your real domain tables)
CREATE TABLE IF NOT EXISTS entities (
    id              SERIAL PRIMARY KEY,
    code            VARCHAR(50)  NOT NULL UNIQUE,
    name            VARCHAR(200) NOT NULL,
    category        VARCHAR(100),
    status          VARCHAR(50)  DEFAULT 'ACTIVE',
    metadata        JSONB,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS entity_metrics (
    id              SERIAL PRIMARY KEY,
    entity_id       INTEGER NOT NULL REFERENCES entities(id),
    metric_name     VARCHAR(100) NOT NULL,
    metric_value    NUMERIC(15,4),
    measured_at     TIMESTAMP NOT NULL,
    period          VARCHAR(20),  -- e.g. DAILY, WEEKLY, MONTHLY
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS events (
    id              SERIAL PRIMARY KEY,
    entity_id       INTEGER REFERENCES entities(id),
    event_type      VARCHAR(100) NOT NULL,
    severity        VARCHAR(20)  DEFAULT 'INFO',  -- INFO, WARNING, CRITICAL
    description     TEXT,
    status          VARCHAR(50)  DEFAULT 'OPEN',
    occurred_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at     TIMESTAMP,
    created_at      TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_entity_metrics_entity_id   ON entity_metrics(entity_id);
CREATE INDEX IF NOT EXISTS idx_entity_metrics_measured_at ON entity_metrics(measured_at);
CREATE INDEX IF NOT EXISTS idx_events_entity_id           ON events(entity_id);
CREATE INDEX IF NOT EXISTS idx_events_occurred_at         ON events(occurred_at);
CREATE INDEX IF NOT EXISTS idx_events_status              ON events(status);
CREATE INDEX IF NOT EXISTS idx_jargon_short_form          ON jargon_dictionary(LOWER(short_form));

-- Auto-update updated_at
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_jargon_updated_at
    BEFORE UPDATE ON jargon_dictionary
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_entities_updated_at
    BEFORE UPDATE ON entities
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- Read-only user for MCP Server + Business Data API
-- Replace password via Secret Manager in production
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'app_readonly') THEN
        CREATE USER app_readonly WITH PASSWORD 'change-via-secret-manager';
    END IF;
END $$;

GRANT CONNECT ON DATABASE current_database() TO app_readonly;
GRANT USAGE ON SCHEMA public TO app_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO app_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO app_readonly;
