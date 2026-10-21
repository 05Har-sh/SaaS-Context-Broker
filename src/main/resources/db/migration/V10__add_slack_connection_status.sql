ALTER TABLE slack_integration
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_slack_integration_active
    ON slack_integration (tenant_id, is_active);
