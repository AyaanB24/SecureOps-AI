package com.secureops.pipeline.dto;

import com.secureops.pipeline.PipelineProvider;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * FILE: src/main/java/com/secureops/pipeline/dto/CreatePipelineRequest.java
 * PURPOSE: DTO for creating a new pipeline via REST API.
 * WHY IT EXISTS: Validates and structures incoming POST request data; never exposes JPA entities.
 * DEPENDENCIES: Used by PipelineController; validated automatically by Spring's @Valid annotation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePipelineRequest {

    @NotNull(message = "Provider is required")
    private PipelineProvider provider;

    @NotBlank(message = "Job name is required")
    @Size(min = 1, max = 255, message = "Job name must be between 1 and 255 characters")
    private String jobName;

    @NotNull(message = "Build number is required")
    @Positive(message = "Build number must be positive")
    private Long buildNumber;

    @NotBlank(message = "Branch is required")
    @Size(min = 1, max = 255, message = "Branch must be between 1 and 255 characters")
    private String branch;

    @Size(max = 255, message = "Commit SHA must be at most 255 characters")
    private String commitSha;

}
