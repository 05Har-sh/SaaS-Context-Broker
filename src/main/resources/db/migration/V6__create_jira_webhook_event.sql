CREATE TABLE jira_webhook_event (
                                    id BIGSERIAL PRIMARY KEY,
                                    tenant_id VARCHAR(255) NOT NULL,
                                    webhook_identifier VARCHAR(255) NOT NULL,
                                    received_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                    CONSTRAINT uk_jira_webhook_event_tenant_identifier
                                        UNIQUE (tenant_id, webhook_identifier)
);

CREATE INDEX idx_jira_webhook_event_tenant
    ON jira_webhook_event (tenant_id);