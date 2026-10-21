ALTER TABLE jira_integration
    ADD COLUMN IF NOT EXISTS connection_status VARCHAR(32) NOT NULL DEFAULT 'CONNECTED',
    ADD COLUMN IF NOT EXISTS webhook_health VARCHAR(32) NOT NULL DEFAULT 'HEALTHY',
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE;

UPDATE jira_integration
SET connection_status = 'CONNECTED',
    webhook_health = 'HEALTHY'
WHERE connection_status IS NULL OR webhook_health IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_jira_integration_tenant_cloud
    ON jira_integration (tenant_id, cloud_id)
    WHERE cloud_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_jira_integration_status
    ON jira_integration (tenant_id, connection_status);
