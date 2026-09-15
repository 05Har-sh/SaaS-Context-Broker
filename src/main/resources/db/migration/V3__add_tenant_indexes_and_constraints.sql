-- Each incident key must be unique within a tenant.
ALTER TABLE incident_entity
    ADD CONSTRAINT uq_incident_tenant_key
        UNIQUE (tenant_id, incident_key);


-- Supports tenant-scoped incident event timeline queries.
CREATE INDEX idx_incident_event_tenant_incident_timestamp
    ON incident_event_entity (tenant_id, incident_key, timestamp);


-- Supports tenant-scoped timeline event queries.
CREATE INDEX idx_timeline_event_tenant_incident_timestamp
    ON timeline_event_entity (tenant_id, incident_key, timestamp);


-- Supports tenant-scoped webhook configuration lookups.
CREATE INDEX idx_webhook_config_tenant
    ON webhook_config (tenant_id);