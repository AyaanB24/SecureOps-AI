package com.secureops.project.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.secureops.project.Project;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/project/dto/ProjectResponse.java
 * PURPOSE: DTO for API responses containing project data.
 * WHY IT EXISTS: Serializes Project entity to JSON without exposing internal structure; controls what client sees.
 * DEPENDENCIES: Used by ProjectController to build HTTP responses; Jackson handles serialization.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    private UUID id;

    private String name;

    private String repositoryUrl;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * Convert Project entity to ProjectResponse DTO.
     */
    public static ProjectResponse fromEntity(Project project) {
        return new ProjectResponse(
            project.getId(),
            project.getName(),
            project.getRepositoryUrl(),
            project.getCreatedAt()
        );
    }

}
