CREATE TABLE tenants (
                         tenant_id VARCHAR(255) PRIMARY KEY,
                         name VARCHAR(255) NOT NULL,
                         created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Preserve the tenant used by existing application data.
INSERT INTO tenants (tenant_id, name)
VALUES ('tenant-001', 'Default Tenant');