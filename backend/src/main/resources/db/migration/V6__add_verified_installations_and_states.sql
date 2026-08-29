-- Flyway migration V6: Add verified installations, single-use installation states, and repository installation anchor

-- 1. Installation States table for single-use opaque CSRF/replay defense
CREATE TABLE installation_states (
    id UUID PRIMARY KEY,
    state_hash VARCHAR(64) NOT NULL,
    user_id UUID NOT NULL,
    session_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_installation_states_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_installation_states_hash UNIQUE (state_hash),
    CONSTRAINT chk_installation_states_status CHECK (status IN ('ACTIVE', 'CONSUMED', 'EXPIRED'))
);

CREATE INDEX idx_installation_states_lookup ON installation_states(state_hash, status, expires_at);
CREATE INDEX idx_installation_states_user_id ON installation_states(user_id);

-- 2. User Installations table for verified user-installation associations
CREATE TABLE user_installations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    github_user_id BIGINT NOT NULL,
    installation_id BIGINT NOT NULL,
    account_login VARCHAR(255) NOT NULL,
    account_type VARCHAR(64) NOT NULL,
    verified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_user_installations_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_installations_user_inst UNIQUE (user_id, installation_id)
);

CREATE INDEX idx_user_installations_user_id ON user_installations(user_id);
CREATE INDEX idx_user_installations_inst_id ON user_installations(installation_id);

-- 3. Add installation_id anchor to repositories table
ALTER TABLE repositories ADD COLUMN installation_id BIGINT;
CREATE INDEX idx_repositories_github_repo_installation ON repositories(github_repo_id, installation_id);
