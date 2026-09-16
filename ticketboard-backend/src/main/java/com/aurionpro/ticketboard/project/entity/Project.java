package com.aurionpro.ticketboard.project.entity;

import com.aurionpro.ticketboard.client.entity.Client;
import com.aurionpro.ticketboard.common.entity.BaseEntity;
import com.aurionpro.ticketboard.project.enums.ProjectHealth;
import com.aurionpro.ticketboard.project.enums.ProjectPriority;
import com.aurionpro.ticketboard.project.enums.ProjectStatus;
import com.aurionpro.ticketboard.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects", indexes = {
    @Index(name = "idx_project_status", columnList = "status"),
    @Index(name = "idx_project_health", columnList = "health"),
    @Index(name = "idx_project_manager_id", columnList = "project_manager_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_code", nullable = false, unique = true, length = 50)
    private String projectCode;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_manager_id")
    private User projectManager;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "planned_end_date")
    private LocalDate plannedEndDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 30)
    @Builder.Default
    private ProjectPriority priority = ProjectPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ProjectStatus status = ProjectStatus.PLANNED;

    @Enumerated(EnumType.STRING)
    @Column(name = "health", nullable = false, length = 30)
    @Builder.Default
    private ProjectHealth health = ProjectHealth.GREEN;

    @Column(name = "budget")
    private Double budget;

    @Column(name = "estimated_hours")
    @Builder.Default
    private Double estimatedHours = 0.0;

    @Column(name = "actual_hours")
    @Builder.Default
    private Double actualHours = 0.0;

    @Column(name = "completion_percentage")
    @Builder.Default
    private Double completionPercentage = 0.0;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProjectMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Milestone> milestones = new ArrayList<>();
}
