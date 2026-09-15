ALTER TABLE incident_entity
    ADD COLUMN tenant_id VARCHAR(255);

ALTER TABLE incident_event_entity
    ADD COLUMN tenant_id VARCHAR(255);

ALTER TABLE timeline_event_entity
    ADD COLUMN tenant_id VARCHAR(255);

ALTER TABLE webhook_config
    ADD COLUMN tenant_id VARCHAR(255);


UPDATE incident_entity
SET tenant_id = 'tenant-001'
WHERE tenant_id IS NULL;

UPDATE incident_event_entity
SET tenant_id = 'tenant-001'
WHERE tenant_id IS NULL;

UPDATE timeline_event_entity
SET tenant_id = 'tenant-001'
WHERE tenant_id IS NULL;

UPDATE webhook_config
SET tenant_id = 'tenant-001'
WHERE tenant_id IS NULL;


ALTER TABLE incident_entity
    ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE incident_event_entity
    ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE timeline_event_entity
    ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE webhook_config
    ALTER COLUMN tenant_id SET NOT NULL;