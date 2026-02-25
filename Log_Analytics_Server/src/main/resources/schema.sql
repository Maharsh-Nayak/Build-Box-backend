-- Create analytics_events table if it doesn't exist
CREATE TABLE IF NOT EXISTS analytics_events (
    id VARCHAR(255) PRIMARY KEY,
    project_id VARCHAR(255),
    account_id VARCHAR(255),
    event_type VARCHAR(255),
    path VARCHAR(2048),
    method VARCHAR(10),
    status INTEGER,
    duration INTEGER,
    bytes_in BIGINT,
    bytes_out BIGINT,
    source VARCHAR(50),
    ip VARCHAR(45),
    user_agent TEXT,
    timestamp TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_project_timestamp (project_id, timestamp),
    INDEX idx_account_timestamp (account_id, timestamp),
    INDEX idx_timestamp (timestamp)
);
