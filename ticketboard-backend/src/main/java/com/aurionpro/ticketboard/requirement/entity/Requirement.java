package com.aurionpro.ticketboard.requirement.entity;

import com.aurionpro.ticketboard.common.entity.BaseEntity;
import com.aurionpro.ticketboard.project.entity.Project;
import com.aurionpro.ticketboard.requirement.enums.RequirementPriority;
import com.aurionpro.ticketboard.requirement.enums.RequirementStatus;
import com.aurionpro.ticketboard.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "requirements", indexes = {
    @Index(name = "idx_requirement_project_id", columnList = "project_id"),
    @Index(name = "idx_requirement_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Requirement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "req_number", nullable = false, unique = true, length = 50)
    private String reqNumber;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "business_objective", columnDefinition = "TEXT")
    private String businessObjective;

    @Column(name = "acceptance_criteria", columnDefinition = "TEXT")
    private String acceptanceCriteria;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 30)
    @Builder.Default
    private RequirementPriority priority = RequirementPriority.MEDIUM;

    @Column(name = "requester", length = 100)
    private String requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "estimated_effort_hours")
    @Builder.Default
    private Double estimatedEffortHours = 0.0;

    @Column(name = "actual_effort_hours")
    @Builder.Default
    private Double actualEffortHours = 0.0;

    @Column(name = "planned_start_date")
    private LocalDate plannedStartDate;

    @Column(name = "planned_end_date")
    private LocalDate plannedEndDate;

    @Column(name = "actual_start_date")
    private LocalDate actualStartDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private RequirementStatus status = RequirementStatus.DRAFT;

    @Column(name = "delivery_version", length = 50)
    private String deliveryVersion;

    @Column(name = "scope_version")
    @Builder.Default
    private Integer scopeVersion = 1;

    @Column(name = "original_estimate_hours")
    private Double originalEstimateHours;

    @Column(name = "scope_creep_flag")
    @Builder.Default
    private Boolean scopeCreepFlag = false;
}
