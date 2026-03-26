-- =============================================================================
-- V1: Initial schema for Personal AI Coach
-- Creates core tables for users, preferences, todos, menu plans,
-- and conversation history.
-- =============================================================================

-- ── Users ────────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    google_id       VARCHAR(255) NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    picture_url     VARCHAR(1024),
    locale          VARCHAR(10) DEFAULT 'sv',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_google_id ON users(google_id);
CREATE INDEX idx_users_email ON users(email);

-- ── User Preferences ─────────────────────────────────────────────────────────
CREATE TABLE user_preferences (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category        VARCHAR(50) NOT NULL,
    preference_key  VARCHAR(100) NOT NULL,
    preference_value TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, category, preference_key)
);

CREATE INDEX idx_user_prefs_user_id ON user_preferences(user_id);

-- ── Todo Items ───────────────────────────────────────────────────────────────
CREATE TABLE todo_items (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           VARCHAR(500) NOT NULL,
    description     TEXT,
    category        VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    priority        VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    due_date        DATE,
    source_agent    VARCHAR(50),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_todos_user_id ON todo_items(user_id);
CREATE INDEX idx_todos_status ON todo_items(status);
CREATE INDEX idx_todos_due_date ON todo_items(due_date);

-- ── Menu Plans ───────────────────────────────────────────────────────────────
CREATE TABLE menu_plans (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    week_year       VARCHAR(10) NOT NULL,
    day_of_week     VARCHAR(10) NOT NULL,
    meal_type       VARCHAR(20) NOT NULL DEFAULT 'DINNER',
    recipe_name     VARCHAR(500) NOT NULL,
    recipe_details  TEXT,
    servings        INT DEFAULT 4,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_menu_plans_user_week ON menu_plans(user_id, week_year);

-- ── Shopping List Items ──────────────────────────────────────────────────────
CREATE TABLE shopping_list_items (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    menu_plan_id    BIGINT REFERENCES menu_plans(id) ON DELETE SET NULL,
    item_name       VARCHAR(255) NOT NULL,
    quantity        VARCHAR(50),
    category        VARCHAR(50),
    checked         BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_shopping_user_id ON shopping_list_items(user_id);

-- ── Conversation History ─────────────────────────────────────────────────────
CREATE TABLE conversation_history (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    conversation_id VARCHAR(255) NOT NULL,
    agent_type      VARCHAR(50) NOT NULL,
    role            VARCHAR(20) NOT NULL,
    content         TEXT NOT NULL,
    model_used      VARCHAR(100),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_conv_user_id ON conversation_history(user_id);
CREATE INDEX idx_conv_conversation_id ON conversation_history(conversation_id);
CREATE INDEX idx_conv_agent_type ON conversation_history(agent_type);
