package com.secureops.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FILE: src/main/java/com/secureops/project/dto/CreateProjectRequest.java
 * PURPOSE: DTO for POST /api/projects request body.
 * WHY IT EXISTS: Receives and validates user input without exposing JPA entity; decouples API contract from database.
 * DEPENDENCIES: Validated by Jakarta Validation; used by ProjectController.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectRequest {

    @NotBlank(message = "Project name is required")
    @Size(min = 1, max = 255, message = "Project name must be between 1 and 255 characters")
    private String name;

    @Size(max = 512, message = "Repository URL must not exceed 512 characters")
    private String repositoryUrl;

}
