-- Scan Pilot Core Schema Migration V1
-- Standard ANSI / PostgreSQL SQL Compatible with PostgreSQL 15/16 and H2

-- 1. Users
CREATE TABLE users (
    id UUID PRIMARY KEY,
    github_user_id BIGINT UNIQUE,
    login VARCHAR(255),
    name VARCHAR(255),
    avatar_url VARCHAR(1024),
    email VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);

-- 2. User Sessions
CREATE TABLE user_sessions (
    session_id VARCHAR(64) PRIMARY KEY,
    user_id UUID NOT NULL,
    access_token VARCHAR(512),
    installation_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 3. Repositories
CREATE TABLE repositories (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    github_repo_id BIGINT,
    owner VARCHAR(255),
    name VARCHAR(255),
    full_name VARCHAR(512),
    default_branch VARCHAR(255),
    primary_branch VARCHAR(255),
    is_private BOOLEAN,
    status VARCHAR(64),
    monitored_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_repositories_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_repositories_user_github_repo UNIQUE (user_id, github_repo_id)
);

-- 4. Monitored Branches
CREATE TABLE monitored_branches (
    id UUID PRIMARY KEY,
    repository_id UUID NOT NULL,
    branch_name VARCHAR(255) NOT NULL,
    branch_type VARCHAR(64),
    is_active BOOLEAN,
    created_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_monitored_branches_repo FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE CASCADE,
    CONSTRAINT uq_monitored_branches_repo_branch UNIQUE (repository_id, branch_name)
);

-- 5. Scan Jobs
CREATE TABLE scan_jobs (
    id UUID PRIMARY KEY,
    repository_id UUID NOT NULL,
    branch_name VARCHAR(255),
    scan_mode VARCHAR(64),
    status VARCHAR(64),
    commit_sha VARCHAR(64),
    duration_ms BIGINT,
    error_message TEXT,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_scan_jobs_repo FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE CASCADE
);

-- 6. Scan Checkpoints
CREATE TABLE scan_checkpoints (
    id UUID PRIMARY KEY,
    repository_id UUID NOT NULL,
    branch_name VARCHAR(255),
    verified_commit_sha VARCHAR(64),
    scan_job_id UUID,
    created_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_scan_checkpoints_repo FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE CASCADE,
    CONSTRAINT fk_scan_checkpoints_job FOREIGN KEY (scan_job_id) REFERENCES scan_jobs(id) ON DELETE SET NULL
);

-- 7. Findings
CREATE TABLE findings (
    id UUID PRIMARY KEY,
    repository_id UUID NOT NULL,
    rule_id VARCHAR(255),
    fingerprint VARCHAR(64) NOT NULL,
    severity VARCHAR(64),
    title VARCHAR(512),
    description TEXT,
    lifecycle VARCHAR(64),
    remediation_quality VARCHAR(64),
    first_seen_at TIMESTAMP WITH TIME ZONE,
    last_seen_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_findings_repo FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE CASCADE,
    CONSTRAINT uq_findings_repo_fingerprint UNIQUE (repository_id, fingerprint)
);

-- 8. Finding Locations
CREATE TABLE finding_locations (
    id UUID PRIMARY KEY,
    finding_id UUID NOT NULL,
    file_path VARCHAR(1024),
    start_line INT,
    end_line INT,
    start_column INT,
    end_column INT,
    commit_sha VARCHAR(64),
    author VARCHAR(255),
    is_current_head BOOLEAN,
    detected_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_locations_finding FOREIGN KEY (finding_id) REFERENCES findings(id) ON DELETE CASCADE
);

-- 9. Evidence Items
CREATE TABLE evidence_items (
    id UUID PRIMARY KEY,
    finding_id UUID,
    evidence_type VARCHAR(64),
    masked_secret VARCHAR(512),
    redacted_snippet TEXT,
    verification_status VARCHAR(64),
    source_attribution VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_evidence_finding FOREIGN KEY (finding_id) REFERENCES findings(id) ON DELETE CASCADE
);

-- 10. Coverage Records
CREATE TABLE coverage_records (
    id UUID PRIMARY KEY,
    scan_job_id UUID NOT NULL,
    repository_id UUID NOT NULL,
    branch_name VARCHAR(255),
    total_files INT,
    scanned_files INT,
    skipped_files INT,
    text_files INT,
    binary_files INT,
    undetermined_files INT,
    total_bytes BIGINT,
    coverage_impact VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_coverage_records_job FOREIGN KEY (scan_job_id) REFERENCES scan_jobs(id) ON DELETE CASCADE,
    CONSTRAINT fk_coverage_records_repo FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE CASCADE
);

-- 11. Coverage Items
CREATE TABLE coverage_items (
    id UUID PRIMARY KEY,
    coverage_record_id UUID NOT NULL,
    file_path VARCHAR(1024),
    classification VARCHAR(64),
    size_bytes BIGINT,
    status VARCHAR(64),
    reason_code VARCHAR(128),
    impact VARCHAR(64),
    details TEXT,
    CONSTRAINT fk_coverage_items_record FOREIGN KEY (coverage_record_id) REFERENCES coverage_records(id) ON DELETE CASCADE
);

-- 12. Review Requests
CREATE TABLE review_requests (
    id UUID PRIMARY KEY,
    finding_id UUID,
    repository_id UUID NOT NULL,
    question TEXT,
    context_summary TEXT,
    status VARCHAR(64),
    answer TEXT,
    answered_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_review_requests_finding FOREIGN KEY (finding_id) REFERENCES findings(id) ON DELETE CASCADE,
    CONSTRAINT fk_review_requests_repo FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE CASCADE
);

-- Indexes for performance & query lookups
CREATE INDEX idx_users_login ON users(login);
CREATE INDEX idx_repos_user ON repositories(user_id);
CREATE INDEX idx_branches_repo ON monitored_branches(repository_id);
CREATE INDEX idx_scans_repo_status ON scan_jobs(repository_id, status);
CREATE INDEX idx_findings_repo_lifecycle ON findings(repository_id, lifecycle);
CREATE INDEX idx_findings_fingerprint ON findings(fingerprint);
CREATE INDEX idx_locations_finding ON finding_locations(finding_id);
CREATE INDEX idx_evidence_finding ON evidence_items(finding_id);
CREATE INDEX idx_coverage_job ON coverage_records(scan_job_id);
CREATE INDEX idx_reviews_finding ON review_requests(finding_id);
