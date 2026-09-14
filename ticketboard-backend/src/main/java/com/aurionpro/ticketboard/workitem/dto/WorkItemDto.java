package com.aurionpro.ticketboard.workitem.dto;

import com.aurionpro.ticketboard.workitem.enums.WorkItemPriority;
import com.aurionpro.ticketboard.workitem.enums.WorkItemSeverity;
import com.aurionpro.ticketboard.workitem.enums.WorkItemStatus;
import com.aurionpro.ticketboard.workitem.enums.WorkItemType;
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
public class WorkItemDto {
    private Long id;
    private String ticketNumber;
    private String title;
    private String description;
    private WorkItemType type;
    private WorkItemPriority priority;
    private WorkItemSeverity severity;
    private WorkItemStatus status;
    private Long projectId;
    private String projectCode;
    private String projectName;
    private Long requirementId;
    private String reqNumber;
    private String reqTitle;
    private Long parentTaskId;
    private String parentTaskTitle;
    private Long assigneeId;
    private String assigneeName;
    private Long reporterId;
    private String reporterName;
    private Double estimatedHours;
    private Double actualHours;
    private Double differenceHours;
    private LocalDate startDate;
    private LocalDate dueDate;
    private LocalDate completedDate;

    // Enterprise Delivery Milestones
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

    private LocalDateTime blockedSince;
    private String blockedReason;
    private String blockedOwner;
    private String labels;
    private Integer commentsCount;
    private Integer documentsCount;
    private List<WorkItemDto> subtasks;
    private List<DependencyDto> dependencies;
    private List<TaskDocumentDto> documents;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
