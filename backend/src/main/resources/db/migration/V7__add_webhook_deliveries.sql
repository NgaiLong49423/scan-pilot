-- Flyway migration V7: Add webhook_deliveries table for tracking and deduplicating incoming GitHub webhook deliveries

CREATE TABLE webhook_deliveries (
    id UUID PRIMARY KEY,
    delivery_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    repository_id UUID REFERENCES repositories(id) ON DELETE SET NULL,
    github_repo_id BIGINT,
    installation_id BIGINT,
    branch VARCHAR(255),
    default_branch VARCHAR(255),
    base_branch VARCHAR(255),
    head_branch VARCHAR(255),
    commit_sha VARCHAR(64),
    base_sha VARCHAR(64),
    pr_number INTEGER,
    pr_action VARCHAR(64),
    is_fork BOOLEAN DEFAULT FALSE,
    is_deleted BOOLEAN DEFAULT FALSE,
    is_merged BOOLEAN DEFAULT FALSE,
    status VARCHAR(32) NOT NULL,
    reason_code VARCHAR(64) NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_webhook_deliveries_delivery_id UNIQUE (delivery_id)
);

CREATE INDEX idx_webhook_deliveries_repo_received ON webhook_deliveries(repository_id, received_at DESC);
CREATE INDEX idx_webhook_deliveries_status ON webhook_deliveries(status, processed_at DESC);
