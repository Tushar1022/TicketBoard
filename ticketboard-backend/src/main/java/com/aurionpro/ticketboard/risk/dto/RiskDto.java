package com.aurionpro.ticketboard.risk.dto;

import com.aurionpro.ticketboard.risk.enums.RiskStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskDto {
    private Long id;
    private String riskCode;

    @NotNull(message = "Project ID is required")
    private Long projectId;
    private String projectCode;
    private String projectName;

    @NotBlank(message = "Description is required")
    private String description;

    @Min(1) @Max(5)
    private Integer probability;

    @Min(1) @Max(5)
    private Integer impact;

    private Integer riskScore;
    private Long ownerId;
    private String ownerName;
    private String mitigationPlan;
    private LocalDate targetDate;
    private RiskStatus status;
    private LocalDateTime createdAt;
}
