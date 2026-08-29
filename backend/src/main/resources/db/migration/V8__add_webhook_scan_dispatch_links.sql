-- Flyway migration V8: Add webhook dispatch links, trigger provenance, and queue indexes (Portable H2 & PostgreSQL)

ALTER TABLE scan_jobs ADD COLUMN webhook_delivery_id UUID;
ALTER TABLE scan_jobs ADD COLUMN trigger_type VARCHAR(64) NOT NULL DEFAULT 'MANUAL';
ALTER TABLE scan_jobs ADD COLUMN expected_commit_sha VARCHAR(64);
ALTER TABLE scan_jobs ADD COLUMN pr_number INTEGER;

ALTER TABLE scan_jobs
    ADD CONSTRAINT fk_scan_jobs_webhook_delivery
    FOREIGN KEY (webhook_delivery_id) REFERENCES webhook_deliveries(id) ON DELETE SET NULL;

-- Portable standard UNIQUE constraint (permits multiple NULLs for manual scans, enforces 1 job per webhook delivery)
ALTER TABLE scan_jobs
    ADD CONSTRAINT uq_scan_jobs_webhook_delivery
    UNIQUE (webhook_delivery_id);

-- Optimize FIFO queue queries and per-repo status lookups
CREATE INDEX idx_scan_jobs_repo_status_created
    ON scan_jobs(repository_id, status, created_at ASC);

CREATE INDEX idx_scan_jobs_status_created
    ON scan_jobs(status, created_at ASC);
