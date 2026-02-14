-- Migration to create the alb_listener_rules table for deterministic priority allocation
CREATE TABLE IF NOT EXISTS alb_listener_rules (
    id SERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    listener_arn TEXT NOT NULL,
    priority INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT unique_priority_per_listener UNIQUE (listener_arn, priority),
    CONSTRAINT unique_project_per_listener UNIQUE (listener_arn, project_id)
);

-- Index for faster lookups during allocation
CREATE INDEX idx_alb_listener_rules_lookup ON alb_listener_rules (listener_arn, priority);
