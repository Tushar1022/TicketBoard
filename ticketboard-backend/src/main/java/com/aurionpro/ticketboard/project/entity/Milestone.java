package com.aurionpro.ticketboard.project.entity;

import com.aurionpro.ticketboard.common.entity.BaseEntity;
import com.aurionpro.ticketboard.project.enums.MilestoneProgressSource;
import com.aurionpro.ticketboard.project.enums.MilestoneStatus;
import com.aurionpro.ticketboard.release.entity.Release;
import com.aurionpro.ticketboard.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "milestones", indexes = {
    @Index(name = "idx_milestone_project_id", columnList = "project_id"),
    @Index(name = "idx_milestone_status", columnList = "status"),
    @Index(name = "idx_milestone_flag", columnList = "flag"),
    @Index(name = "idx_milestone_release_id", columnList = "release_id"),
    @Index(name = "idx_milestone_parent_id", columnList = "parent_milestone_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Milestone extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "milestone_code", unique = true, length = 50)
    private String milestoneCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "planned_date")
    private LocalDate plannedDate;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "actual_date")
    private LocalDate actualDate;

    @Column(name = "priority", length = 30)
    private String priority;

    @Column(name = "flag", length = 50)
    private String flag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_milestone_id")
    private Milestone parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "release_id")
    private Release release;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private MilestoneStatus status = MilestoneStatus.PLANNED;

    @Column(name = "completion_percentage")
    @Builder.Default
    private Double completionPercentage = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "progress_source", nullable = false, length = 20)
    @Builder.Default
    private MilestoneProgressSource progressSource = MilestoneProgressSource.MANUAL;
}
