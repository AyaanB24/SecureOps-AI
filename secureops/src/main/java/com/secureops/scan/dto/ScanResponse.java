package com.secureops.scan.dto;

import com.secureops.scan.Scan;
import com.secureops.scan.Environment;
import com.secureops.scan.ScanStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/scan/dto/ScanResponse.java
 * PURPOSE: DTO for returning scan data via REST API.
 * WHY IT EXISTS: Decouples API response structure from JPA entity; includes only relevant fields for clients.
 * DEPENDENCIES: Used by ScanController and ScanService; converts from Scan entity via static factory method.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScanResponse {

    private UUID id;
    private UUID projectId;
    private UUID pipelineId;
    private Environment environment;
    private ScanStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    /**
     * Factory method to convert Scan entity to ScanResponse DTO.
     *
     * @param scan Scan entity
     * @return ScanResponse DTO
     */
    public static ScanResponse fromEntity(Scan scan) {
        return new ScanResponse(
            scan.getId(),
            scan.getProject().getId(),
            scan.getPipeline().getId(),
            scan.getEnvironment(),
            scan.getStatus(),
            scan.getStartedAt(),
            scan.getCompletedAt()
        );
    }

}
