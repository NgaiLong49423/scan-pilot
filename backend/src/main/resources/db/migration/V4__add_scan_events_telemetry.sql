-- Scan Pilot Schema Migration V4: Add scan_events telemetry and durable sequence counter
ALTER TABLE scan_jobs ADD COLUMN next_event_sequence BIGINT NOT NULL DEFAULT 0;

CREATE TABLE scan_events (
    id UUID PRIMARY KEY,
    scan_job_id UUID NOT NULL,
    sequence_number BIGINT NOT NULL,
    stage VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    message_code VARCHAR(64) NOT NULL,
    payload_json VARCHAR(1024),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_scan_events_job FOREIGN KEY (scan_job_id) REFERENCES scan_jobs(id) ON DELETE CASCADE,
    CONSTRAINT uq_scan_events_job_seq UNIQUE (scan_job_id, sequence_number)
);

CREATE INDEX idx_scan_events_job_seq ON scan_events(scan_job_id, sequence_number);
