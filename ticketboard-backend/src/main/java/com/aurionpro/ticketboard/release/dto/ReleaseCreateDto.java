package com.aurionpro.ticketboard.release.dto;

import com.aurionpro.ticketboard.release.enums.ReleaseEnvironment;
import com.aurionpro.ticketboard.release.enums.ReleaseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseCreateDto {

    private String releaseVersion;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Project ID is required")
    private Long projectId;

    private ReleaseEnvironment environment;
    private LocalDate plannedDate;
    private LocalDate actualDate;
    private ReleaseStatus status;
    private String deploymentResult;
    private Boolean rollbackRequired;
    private Long ownerId;
    private List<Long> requirementIds;
    private List<Long> workItemIds;
}
