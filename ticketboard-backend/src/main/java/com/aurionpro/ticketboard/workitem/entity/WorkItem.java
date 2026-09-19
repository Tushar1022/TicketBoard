package com.aurionpro.ticketboard.workitem.entity;

import com.aurionpro.ticketboard.common.entity.BaseEntity;
import com.aurionpro.ticketboard.project.entity.Milestone;
import com.aurionpro.ticketboard.project.entity.Project;
import com.aurionpro.ticketboard.requirement.entity.Requirement;
import com.aurionpro.ticketboard.user.entity.User;
import com.aurionpro.ticketboard.workitem.enums.WorkItemPriority;
import com.aurionpro.ticketboard.workitem.enums.WorkItemSeverity;
import com.aurionpro.ticketboard.workitem.enums.WorkItemStatus;
import com.aurionpro.ticketboard.workitem.enums.WorkItemType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "work_items", indexes = {
    @Index(name = "idx_workitem_project_id", columnList = "project_id"),
    @Index(name = "idx_workitem_assignee_id", columnList = "assignee_id"),
    @Index(name = "idx_workitem_status", columnList = "status"),
    @Index(name = "idx_workitem_type", columnList = "type"),
    @Index(name = "idx_workitem_priority", columnList = "priority")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", nullable = false, unique = true, length = 50)
    private String ticketNumber;

    @Column(name = "title", nullable = false, length = 350)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    @Builder.Default
    private WorkItemType type = WorkItemType.TASK;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 30)
    @Builder.Default
    private WorkItemPriority priority = WorkItemPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 30)
    @Builder.Default
    private WorkItemSeverity severity = WorkItemSeverity.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private WorkItemStatus status = WorkItemStatus.TODO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_id")
    private Requirement requirement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_id")
    private Milestone milestone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_task_id")
    private WorkItem parentTask;

    @OneToMany(mappedBy = "parentTask", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkItem> subtasks = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @Column(name = "estimated_hours")
    @Builder.Default
    private Double estimatedHours = 0.0;

    @Column(name = "actual_hours")
    @Builder.Default
    private Double actualHours = 0.0;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    // Delivery Phase Milestones
    @Column(name = "dev_exit_date")
    private LocalDate devExitDate;

    @Column(name = "sit_exit_date")
    private LocalDate sitExitDate;

    @Column(name = "uat_exit_date")
    private LocalDate uatExitDate;

    @Column(name = "sd_delivery_date")
    private LocalDate sdDeliveryDate;

    @Column(name = "go_live_date")
    private LocalDate goLiveDate;

    @Column(name = "dev_effort_days")
    private Double devEffortDays;

    @Column(name = "qc_effort_days")
    private Double qcEffortDays;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "completion_percentage")
    @Builder.Default
    private Integer completionPercentage = 0;

    @Column(name = "billing_type", length = 50)
    @Builder.Default
    private String billingType = "None";

    @Column(name = "associated_team", length = 150)
    private String associatedTeam;

    @Column(name = "allocated_ba", length = 150)
    private String allocatedBa;

    @Column(name = "jira_task_id", length = 50)
    private String jiraTaskId;

    @Column(name = "jira_status", length = 50)
    @Builder.Default
    private String jiraStatus = "Not Created";

    @Column(name = "tags", length = 255)
    private String tags;

    @Column(name = "reminder", length = 100)
    private String reminder;

    @Column(name = "recurrence", length = 100)
    private String recurrence;

    @Column(name = "blocked_since")
    private LocalDateTime blockedSince;

    @Column(name = "blocked_reason", length = 500)
    private String blockedReason;

    @Column(name = "blocked_owner", length = 150)
    private String blockedOwner;

    @Column(name = "labels", length = 255)
    private String labels;
}
