package com.aurionpro.ticketboard.risk.dto;

import com.aurionpro.ticketboard.risk.enums.IssueSeverity;
import com.aurionpro.ticketboard.risk.enums.IssueStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class IssueDto {
    private Long id;
    private String issueCode;

    @NotNull(message = "Project ID is required")
    private Long projectId;
    private String projectCode;
    private String projectName;

    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    private IssueSeverity severity;
    private IssueStatus status;
    private Long ownerId;
    private String ownerName;
    private String resolution;
    private String classification;
    private String stepsToReproduce;
    private Double crValue;
    private Double crManDays;
    private LocalDate dueDate;
    private List<String> linkedTaskIds;
    private LocalDateTime createdAt;
}
