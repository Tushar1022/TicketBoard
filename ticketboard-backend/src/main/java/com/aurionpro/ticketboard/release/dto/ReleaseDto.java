package com.aurionpro.ticketboard.release.dto;

import com.aurionpro.ticketboard.release.enums.ReleaseEnvironment;
import com.aurionpro.ticketboard.release.enums.ReleaseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseDto {
    private Long id;
    private String releaseVersion;
    private String title;
    private String description;
    private Long projectId;
    private String projectCode;
    private String projectName;
    private ReleaseEnvironment environment;
    private LocalDate plannedDate;
    private LocalDate actualDate;
    private ReleaseStatus status;
    private String deploymentResult;
    private Boolean rollbackRequired;
    private Long ownerId;
    private String ownerName;
    private List<Long> requirementIds;
    private List<Long> workItemIds;
    private Integer itemCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
