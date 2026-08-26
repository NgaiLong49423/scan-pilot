-- Scan Pilot Schema Migration V3: Add reason_code and limit_hit_value to coverage_records
ALTER TABLE coverage_records ADD COLUMN reason_code VARCHAR(64);
ALTER TABLE coverage_records ADD COLUMN limit_hit_value BIGINT;
