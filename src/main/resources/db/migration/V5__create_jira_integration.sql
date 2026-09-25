CREATE TABLE jira_integration (
                                  id BIGSERIAL PRIMARY KEY,
                                  tenant_id VARCHAR(255) NOT NULL,
                                  jira_webhook_id VARCHAR(255) NOT NULL UNIQUE,
                                  webhook_secret VARCHAR(255) NOT NULL
);

CREATE INDEX idx_jira_integration_tenant
    ON jira_integration (tenant_id);