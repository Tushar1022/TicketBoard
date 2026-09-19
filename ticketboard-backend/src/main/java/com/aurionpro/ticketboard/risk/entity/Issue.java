package com.aurionpro.ticketboard.risk.entity;

import com.aurionpro.ticketboard.common.entity.BaseEntity;
import com.aurionpro.ticketboard.project.entity.Milestone;
import com.aurionpro.ticketboard.project.entity.Project;
import com.aurionpro.ticketboard.risk.enums.IssueSeverity;
import com.aurionpro.ticketboard.risk.enums.IssueStatus;
import com.aurionpro.ticketboard.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "issues", indexes = {
    @Index(name = "idx_issue_project_id", columnList = "project_id"),
    @Index(name = "idx_issue_status", columnList = "status"),
    @Index(name = "idx_issue_severity", columnList = "severity"),
    @Index(name = "idx_issue_assignee_id", columnList = "assignee_id"),
    @Index(name = "idx_issue_milestone_id", columnList = "milestone_id"),
    @Index(name = "idx_issue_due_date", columnList = "due_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Issue extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "issue_code", nullable = false, unique = true, length = 50)
    private String issueCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_id")
    private Milestone milestone;

    @Column(name = "title", length = 300)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 30)
    @Builder.Default
    private IssueSeverity severity = IssueSeverity.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private IssueStatus status = IssueStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Column(name = "priority", length = 30)
    private String priority;

    @Column(name = "classification", length = 100)
    private String classification;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "environment", length = 50)
    private String environment;

    @Column(name = "affected_module", length = 150)
    private String affectedModule;

    @Column(name = "affected_version", length = 50)
    private String affectedVersion;

    @Column(name = "expected_behavior", columnDefinition = "TEXT")
    private String expectedBehavior;

    @Column(name = "actual_behavior", columnDefinition = "TEXT")
    private String actualBehavior;

    @Column(name = "steps_to_reproduce", columnDefinition = "TEXT")
    private String stepsToReproduce;

    @Column(name = "resolution", columnDefinition = "TEXT")
    private String resolution;

    @Column(name = "acceptance_criteria", columnDefinition = "TEXT")
    private String acceptanceCriteria;

    @Column(name = "cr_value")
    private Double crValue;

    @Column(name = "cr_man_days")
    private Double crManDays;

    @Column(name = "estimated_fix_hours")
    private Double estimatedFixHours;

    @Column(name = "percentage")
    @Builder.Default
    private Integer percentage = 0;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "linked_task_ids", length = 500)
    private String linkedTaskIds;
}