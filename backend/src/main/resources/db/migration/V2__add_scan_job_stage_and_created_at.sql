-- Scan Pilot Schema Migration V2: Add scan job stage, timestamps, worker instance id, and heartbeat
-- Standard ANSI / PostgreSQL SQL Compatible with PostgreSQL 15/16 and H2

ALTER TABLE scan_jobs ADD COLUMN stage VARCHAR(64);
ALTER TABLE scan_jobs ADD COLUMN created_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE scan_jobs ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE scan_jobs ADD COLUMN worker_instance_id VARCHAR(64);
ALTER TABLE scan_jobs ADD COLUMN heartbeat_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_scan_jobs_active ON scan_jobs (repository_id, status);
CREATE INDEX idx_scan_jobs_heartbeat ON scan_jobs (status, heartbeat_at);
