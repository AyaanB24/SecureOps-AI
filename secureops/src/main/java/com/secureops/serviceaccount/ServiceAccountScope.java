package com.secureops.serviceaccount;

/**
 * FILE: src/main/java/com/secureops/serviceaccount/ServiceAccountScope.java
 * PURPOSE: Enumerates the permissions/scopes granted to service accounts.
 * WHY IT EXISTS: Implements least privilege - Jenkins only gets minimal required permissions.
 * 
 * SCOPES:
 * - JENKINS_CI: Full access to project management, scan creation, report upload, decision retrieval
 * - GITHUB_WEBHOOK: Read-only access to projects and decisions (for GitHub CI)
 * - GITLAB_CI: Full access similar to Jenkins
 */
public enum ServiceAccountScope {
    /**
     * Jenkins CI/CD automation - allows:
     * - GET /api/projects (find/list projects)
     * - GET /api/projects/{id} (get specific project)
     * - POST /api/projects (create projects if needed)
     * - POST /api/projects/{id}/pipelines (register pipelines)
     * - POST /api/projects/{id}/scans (create scans)
     * - POST /api/scans/{id}/reports (upload Trivy reports)
     * - GET /api/scans/{id}/decision (get PASS/BLOCK)
     * - GET /api/scans/{id}/risk (get risk score)
     */
    JENKINS_CI("Jenkins CI/CD Automation", "projects.read,projects.create,scans.create,reports.upload,decisions.read"),

    /**
     * GitHub Actions - allows:
     * - GET /api/projects?name=X (find projects)
     * - GET /api/scans/{id}/decision (get decision)
     * - POST /api/scans/{id}/reports (upload reports) - read-only for status checks
     */
    GITHUB_ACTIONS("GitHub Actions CI", "projects.read,scans.read,decisions.read"),

    /**
     * GitLab CI - similar to Jenkins
     */
    GITLAB_CI("GitLab CI/CD Automation", "projects.read,projects.create,scans.create,reports.upload,decisions.read"),

    /**
     * Webhook receiver - typically read-only
     */
    WEBHOOK_RECEIVER("Generic Webhook Receiver", "scans.read,decisions.read");

    private final String displayName;
    private final String permissions;

    ServiceAccountScope(String displayName, String permissions) {
        this.displayName = displayName;
        this.permissions = permissions;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPermissions() {
        return permissions;
    }

    /**
     * Check if this scope has a specific permission.
     */
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
}
