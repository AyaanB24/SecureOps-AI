-- Add OAuth support fields to user table
ALTER TABLE "user" ADD COLUMN github_id VARCHAR(255) UNIQUE;
ALTER TABLE "user" ADD COLUMN google_id VARCHAR(255) UNIQUE;
ALTER TABLE "user" ADD COLUMN profile_picture_url VARCHAR(500);
ALTER TABLE "user" ADD COLUMN auth_provider VARCHAR(50) DEFAULT 'email';

-- Create indexes for OAuth ID lookups
CREATE INDEX idx_user_github_id ON "user"(github_id);
CREATE INDEX idx_user_google_id ON "user"(google_id);
