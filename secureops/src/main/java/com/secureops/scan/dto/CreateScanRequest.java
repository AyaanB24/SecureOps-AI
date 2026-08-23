package com.secureops.scan.dto;

import com.secureops.scan.Environment;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

/**
 * FILE: src/main/java/com/secureops/scan/dto/CreateScanRequest.java
 * PURPOSE: DTO for creating a new scan via REST API.
 * WHY IT EXISTS: Validates and structures incoming POST request data; never exposes JPA entities.
 * DEPENDENCIES: Used by ScanController; validated automatically by Spring's @Valid annotation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateScanRequest {

    @NotNull(message = "Pipeline ID is required")
    private UUID pipelineId;

    @NotNull(message = "Environment is required")
    private Environment environment;

}
