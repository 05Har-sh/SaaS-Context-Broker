ALTER TABLE jira_integration
    ALTER COLUMN webhook_secret DROP NOT NULL;

ALTER TABLE jira_integration
    ADD COLUMN integration_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN cloud_id VARCHAR(255),
    ADD COLUMN site_url VARCHAR(500),
    ADD COLUMN site_name VARCHAR(255),
    ADD COLUMN access_token_encrypted TEXT,
    ADD COLUMN refresh_token_encrypted TEXT;