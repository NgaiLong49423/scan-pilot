-- Flyway migration V5: Finding to GitHub Issue Linkage with Durable State Machine
CREATE TABLE finding_issue_links (
    id UUID PRIMARY KEY,
    finding_id UUID NOT NULL,
    repository_id UUID NOT NULL,
    state VARCHAR(32) NOT NULL, -- 'PENDING', 'CREATED', 'UNKNOWN', 'FAILED'
    github_issue_number INT,
    github_issue_url VARCHAR(1024),
    idempotency_marker VARCHAR(128) NOT NULL,
    failure_reason VARCHAR(64),
    created_by_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_issue_links_finding FOREIGN KEY (finding_id) REFERENCES findings(id) ON DELETE CASCADE,
    CONSTRAINT fk_issue_links_repo FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE CASCADE,
    CONSTRAINT uq_issue_links_finding UNIQUE (finding_id)
);

CREATE INDEX idx_issue_links_repo ON finding_issue_links(repository_id);
CREATE INDEX idx_issue_links_finding ON finding_issue_links(finding_id);
CREATE INDEX idx_issue_links_state ON finding_issue_links(state);
