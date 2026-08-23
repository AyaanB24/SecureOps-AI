package com.secureops.pipeline;

/**
 * FILE: src/main/java/com/secureops/pipeline/PipelineProvider.java
 * PURPOSE: Enum representing supported CI/CD pipeline providers.
 * WHY IT EXISTS: Normalizes provider names and ensures type-safety for pipeline creation.
 * DEPENDENCIES: Used by Pipeline entity and PipelineService.
 */
public enum PipelineProvider {
    JENKINS,
    GITHUB_ACTIONS,
    GITLAB_CI,
    AZURE_PIPELINES,
    CIRCLECI,
    TRAVIS_CI
}
