-- ============================================================================
-- V005: Add Service Account Support for Jenkins Authentication
-- ============================================================================
-- Purpose: Create service_account table for API key-based authentication
-- Phase: Phase 7 - Jenkins Authentication Integration
-- Date: 2026-10-29
--
-- Changes:
-- - Create service_account table
-- - Create index on name and enabled status
-- - Service accounts used by CI/CD tools (Jenkins, GitHub Actions, etc.)
-- - Each account has a single API key (hashed with BCrypt)
-- - API keys are immutable; regeneration creates new hash
-- ============================================================================

CREATE TABLE IF NOT EXISTS service_account (
    id uuid PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    name varchar(255) NOT NULL UNIQUE,
    description varchar(1000),
    api_key_hash varchar(255) NOT NULL,
    scope varchar(50) NOT NULL,
    enabled boolean NOT NULL DEFAULT true,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at timestamp,
    last_used_at timestamp,
    created_by varchar(255)
);

-- Indexes for common queries
CREATE INDEX idx_service_account_name ON service_account(name);
CREATE INDEX idx_service_account_enabled ON service_account(enabled);
CREATE INDEX idx_service_account_scope ON service_account(scope);
CREATE INDEX idx_service_account_created ON service_account(created_at DESC);

-- Comments for documentation
COMMENT ON TABLE service_account IS 'Service accounts for CI/CD tools (Jenkins, GitHub Actions) with API key authentication';
COMMENT ON COLUMN service_account.id IS 'Unique identifier (UUID)';
COMMENT ON COLUMN service_account.name IS 'Friendly name (e.g., "jenkins", "github-actions")';
COMMENT ON COLUMN service_account.api_key_hash IS 'BCrypt hash of API key (never stored in plaintext)';
COMMENT ON COLUMN service_account.scope IS 'Permission scope (JENKINS_CI, GITHUB_ACTIONS, GITLAB_CI, WEBHOOK_RECEIVER)';
COMMENT ON COLUMN service_account.enabled IS 'Whether this account is active (can be disabled without deletion)';
COMMENT ON COLUMN service_account.expires_at IS 'Optional expiration date (null = never expires)';
COMMENT ON COLUMN service_account.last_used_at IS 'Audit trail: when API key was last used';

-- Initial test account (OPTIONAL - comment out in production)
-- To generate a test API key: 
-- 1. Don't run this INSERT
-- 2. Use POST /api/admin/service-accounts endpoint to create account
-- 3. This ensures key is properly hashed and only shown once
--
-- INSERT INTO service_account (name, description, api_key_hash, scope, enabled, created_by)
-- VALUES (
--     'jenkins-test',
--     'Test Jenkins CI/CD account (development only)',
--     '$2a$10$N9qo8uLOickgx2ZMRZoHK.0RnMEqWVNEhhhx7U0C7e1YG3AKnx.B2',  -- BCrypt of "test-api-key"
--     'JENKINS_CI',
--     true,
--     'SYSTEM'
-- );

COMMIT;
