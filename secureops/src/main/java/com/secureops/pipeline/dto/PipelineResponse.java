package com.secureops.pipeline.dto;

import com.secureops.pipeline.Pipeline;
import com.secureops.pipeline.PipelineProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/pipeline/dto/PipelineResponse.java
 * PURPOSE: DTO for returning pipeline data via REST API.
 * WHY IT EXISTS: Decouples API response structure from JPA entity; includes only relevant fields for clients.
 * DEPENDENCIES: Used by PipelineController and PipelineService; converts from Pipeline entity via static factory method.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PipelineResponse {

    private UUID id;
    private UUID projectId;
    private PipelineProvider provider;
    private String jobName;
    private Long buildNumber;
    private String branch;
    private String commitSha;
    private LocalDateTime createdAt;

    /**
     * Factory method to convert Pipeline entity to PipelineResponse DTO.
     *
     * @param pipeline Pipeline entity
     * @return PipelineResponse DTO
     */
    public static PipelineResponse fromEntity(Pipeline pipeline) {
        return new PipelineResponse(
            pipeline.getId(),
            pipeline.getProject().getId(),
            pipeline.getProvider(),
            pipeline.getJobName(),
            pipeline.getBuildNumber(),
            pipeline.getBranch(),
            pipeline.getCommitSha(),
            pipeline.getCreatedAt()
        );
    }

}
