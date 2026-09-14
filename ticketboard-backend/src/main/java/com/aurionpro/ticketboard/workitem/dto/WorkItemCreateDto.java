package com.aurionpro.ticketboard.workitem.dto;

import com.aurionpro.ticketboard.workitem.enums.WorkItemPriority;
import com.aurionpro.ticketboard.workitem.enums.WorkItemSeverity;
import com.aurionpro.ticketboard.workitem.enums.WorkItemStatus;
import com.aurionpro.ticketboard.workitem.enums.WorkItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkItemCreateDto {

    private String ticketNumber;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    private WorkItemType type;
    private WorkItemPriority priority;
    private WorkItemSeverity severity;
    private WorkItemStatus status;

    @NotNull(message = "Project ID is required")
    private Long projectId;

    private Long requirementId;
    private Long parentTaskId;
    private Long assigneeId;
    private Long reporterId;
    private Double estimatedHours;
    private LocalDate startDate;
    private LocalDate dueDate;

    // Delivery Milestones & Zoho fields
    private LocalDate devExitDate;
    private LocalDate sitExitDate;
    private LocalDate uatExitDate;
    private LocalDate sdDeliveryDate;
    private LocalDate goLiveDate;
    private Double devEffortDays;
    private Double qcEffortDays;
    private Integer durationDays;
    private Integer completionPercentage;
    private String billingType;
    private String associatedTeam;
    private String jiraTaskId;
    private String jiraStatus;
    private String tags;
    private String reminder;
    private String recurrence;
    private String labels;
}
