-- ============================================================
-- V1: Create all tables for the IT Incident Intelligence Platform
-- ============================================================
-- This migration is owned by the incident-mcp-server and creates
-- the full schema. The incident-agent-app connects to the same
-- database but relies on this migration having run first.

-- Service registry
CREATE TABLE services (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    team        VARCHAR(100),
    dependencies VARCHAR(500),
    critical    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Operational runbook knowledge base
CREATE TABLE runbooks (
    id           BIGSERIAL    PRIMARY KEY,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    service_name VARCHAR(100),
    content      TEXT         NOT NULL,
    tags         VARCHAR(500),
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Simulated log store (replaces a real log aggregation system in production)
CREATE TABLE simulated_logs (
    id           BIGSERIAL   PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL,
    level        VARCHAR(20)  NOT NULL,
    message      TEXT         NOT NULL,
    metadata     TEXT,        -- JSON string
    timestamp    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_log_service_time ON simulated_logs (service_name, timestamp DESC);
CREATE INDEX idx_log_level        ON simulated_logs (level);

-- Simulated metrics store (replaces a real time-series DB in production)
CREATE TABLE simulated_metrics (
    id           BIGSERIAL     PRIMARY KEY,
    service_name VARCHAR(100)  NOT NULL,
    metric_name  VARCHAR(100)  NOT NULL,
    value        NUMERIC(15,4) NOT NULL,
    unit         VARCHAR(50),
    timestamp    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_metric_service_time ON simulated_metrics (service_name, timestamp DESC);
CREATE INDEX idx_metric_name         ON simulated_metrics (metric_name);

-- Incident tracker
CREATE TABLE incidents (
    id                BIGSERIAL    PRIMARY KEY,
    title             VARCHAR(255) NOT NULL,
    description       TEXT         NOT NULL,
    severity          VARCHAR(20),
    status            VARCHAR(30)  NOT NULL DEFAULT 'OPEN',
    category          VARCHAR(100),
    affected_services VARCHAR(500),
    reported_by       VARCHAR(100),
    assigned_to       VARCHAR(100),
    resolution        TEXT,
    root_cause        TEXT,
    ai_summary        TEXT,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at       TIMESTAMP
);

-- Incident timeline notes (AUTO = AI-generated, MANUAL = human-written)
CREATE TABLE incident_notes (
    id          BIGSERIAL   PRIMARY KEY,
    incident_id BIGINT      NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    content     TEXT        NOT NULL,
    note_type   VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    author      VARCHAR(100),
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_note_incident ON incident_notes (incident_id);
