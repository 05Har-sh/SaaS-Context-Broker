ALTER TABLE slack_integration
    ADD COLUMN team_name VARCHAR(255),
    ADD COLUMN bot_user_id VARCHAR(255),
    ADD COLUMN access_token_encrypted TEXT;