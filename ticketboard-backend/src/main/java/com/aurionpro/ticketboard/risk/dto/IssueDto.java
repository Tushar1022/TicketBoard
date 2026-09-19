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
    private Long milestoneId;
    private String milestoneName;

    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    private IssueSeverity severity;
    private IssueStatus status;
    private Long reporterId;
    private String reporterName;
    private Long assigneeId;
    private String assigneeName;
    private String priority;
    private String classification;
    private String category;
    private String environment;
    private String affectedModule;
    private String affectedVersion;
    private String expectedBehavior;
    private String actualBehavior;
    private String stepsToReproduce;
    private String resolution;
    private String acceptanceCriteria;
    private Double crValue;
    private Double crManDays;
    private Double estimatedFixHours;
    private Integer percentage;
    private LocalDate dueDate;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private List<String> linkedTaskIds;
    private List<String> linkedIssueIds;
    private Long commentCount;
    private Long watcherCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}