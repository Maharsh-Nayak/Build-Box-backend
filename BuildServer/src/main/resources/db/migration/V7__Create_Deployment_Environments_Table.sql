-- Create table for storing project-specific deployment environment variables
CREATE TABLE IF NOT EXISTS deployment_environments (
    id BIGSERIAL PRIMARY KEY,
    project_id VARCHAR(255) NOT NULL,
    environment_type VARCHAR(50) NOT NULL DEFAULT 'FRONTEND', -- FRONTEND or BACKEND
    key_name VARCHAR(255) NOT NULL,
    key_value TEXT NOT NULL,
    is_secret BOOLEAN DEFAULT false, -- If true, don't log or output
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    UNIQUE(project_id, environment_type, key_name),
    CONSTRAINT fk_project FOREIGN KEY(project_id) REFERENCES projects(slug) ON DELETE CASCADE
);

-- Index for quick lookups
CREATE INDEX idx_deployment_env_project_type ON deployment_environments(project_id, environment_type);
CREATE INDEX idx_deployment_env_project ON deployment_environments(project_id);
