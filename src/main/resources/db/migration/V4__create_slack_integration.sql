CREATE TABLE slack_integration (
    id BIGSERIAL PRIMARY KEY,

    tenant_id VARCHAR(255) NOT NULL,

    team_id VARCHAR(255) NOT NULL UNIQUE,

    signing_secret VARCHAR(255) NOT NULL
);

CREATE INDEX idx_slack_integration_tenant
    ON slack_integration (tenant_id);