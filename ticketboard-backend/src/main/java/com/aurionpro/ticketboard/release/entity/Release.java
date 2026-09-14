package com.aurionpro.ticketboard.release.entity;

import com.aurionpro.ticketboard.common.entity.BaseEntity;
import com.aurionpro.ticketboard.project.entity.Project;
import com.aurionpro.ticketboard.release.enums.ReleaseEnvironment;
import com.aurionpro.ticketboard.release.enums.ReleaseStatus;
import com.aurionpro.ticketboard.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "releases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Release extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "release_version", nullable = false, unique = true, length = 50)
    private String releaseVersion;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment", nullable = false, length = 30)
    @Builder.Default
    private ReleaseEnvironment environment = ReleaseEnvironment.PRODUCTION;

    @Column(name = "planned_date")
    private LocalDate plannedDate;

    @Column(name = "actual_date")
    private LocalDate actualDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ReleaseStatus status = ReleaseStatus.PLANNED;

    @Column(name = "deployment_result", length = 100)
    private String deploymentResult;

    @Column(name = "rollback_required")
    @Builder.Default
    private Boolean rollbackRequired = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @OneToMany(mappedBy = "release", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReleaseItem> items = new ArrayList<>();
}
