package com.aurionpro.ticketboard.risk.entity;

import com.aurionpro.ticketboard.common.entity.BaseEntity;
import com.aurionpro.ticketboard.project.entity.Project;
import com.aurionpro.ticketboard.risk.enums.IssueSeverity;
import com.aurionpro.ticketboard.risk.enums.IssueStatus;
import com.aurionpro.ticketboard.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "issues", indexes = {
    @Index(name = "idx_issue_project_id", columnList = "project_id"),
    @Index(name = "idx_issue_status", columnList = "status"),
    @Index(name = "idx_issue_severity", columnList = "severity")
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
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "resolution", columnDefinition = "TEXT")
    private String resolution;

    @Column(name = "classification", length = 100)
    private String classification;

    @Column(name = "steps_to_reproduce", columnDefinition = "TEXT")
    private String stepsToReproduce;

    @Column(name = "cr_value")
    private Double crValue;

    @Column(name = "cr_man_days")
    private Double crManDays;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "linked_task_ids", length = 500)
    private String linkedTaskIds;
}
