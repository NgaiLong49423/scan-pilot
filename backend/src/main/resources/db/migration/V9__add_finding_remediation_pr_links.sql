-- Flyway migration V9: Finding to GitHub Remediation Pull Request Linkage with Durable State Machine
CREATE TABLE finding_remediation_pr_links (
    id UUID PRIMARY KEY,
    finding_id UUID NOT NULL,
    repository_id UUID NOT NULL,
    source_revision_commit VARCHAR(64) NOT NULL,
    target_branch VARCHAR(255) NOT NULL,
    head_branch VARCHAR(255) NOT NULL,
    state VARCHAR(32) NOT NULL, -- 'PENDING', 'CREATED', 'UNKNOWN', 'FAILED'
    github_pr_number INT,
    github_pr_url VARCHAR(1024),
    idempotency_marker VARCHAR(128) NOT NULL,
    failure_reason VARCHAR(64),
    created_by_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_remediation_pr_finding FOREIGN KEY (finding_id) REFERENCES findings(id) ON DELETE CASCADE,
    CONSTRAINT fk_remediation_pr_repo FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE CASCADE,
    CONSTRAINT uq_remediation_pr_finding_revision UNIQUE (finding_id, source_revision_commit)
);

CREATE INDEX idx_remediation_pr_repo ON finding_remediation_pr_links(repository_id);
CREATE INDEX idx_remediation_pr_finding ON finding_remediation_pr_links(finding_id);
CREATE INDEX idx_remediation_pr_state ON finding_remediation_pr_links(state);