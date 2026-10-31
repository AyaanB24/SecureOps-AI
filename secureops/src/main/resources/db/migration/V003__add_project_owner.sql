-- ============================================================================
-- Phase 5: Add Project Ownership Support
-- ============================================================================
-- This migration adds project ownership tracking to support RBAC.
-- Existing projects are assigned to system account for backward compatibility.
-- ============================================================================

-- Add owner_id column to project table (nullable for backward compatibility)
ALTER TABLE project ADD COLUMN owner_id UUID;

-- Add foreign key constraint from project.owner_id to user.id
ALTER TABLE project 
  ADD CONSTRAINT fk_project_owner 
  FOREIGN KEY (owner_id) 
  REFERENCES "user"(id);

-- Create index for performance on owner lookups
CREATE INDEX idx_project_owner ON project(owner_id);

-- Backfill existing projects with system service account
-- This ensures no projects are left orphaned (owner_id = NULL after migration)
-- The system account is created by a setup script or manually via SQL:
-- INSERT INTO "user" (id, email, name, password_hash, role, enabled) 
--   VALUES (gen_random_uuid(), 'system@secureops.internal', 'SecureOps System', 'N/A', 'ADMIN', true)
-- UPDATE project SET owner_id = :systemUserId WHERE owner_id IS NULL
--
-- For now, we leave the backfill to be done by the application on first run
-- to avoid hardcoding UUIDs in the migration.
