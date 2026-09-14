package com.aurionpro.ticketboard.risk.entity;

import com.aurionpro.ticketboard.common.entity.BaseEntity;
import com.aurionpro.ticketboard.project.entity.Project;
import com.aurionpro.ticketboard.risk.enums.RiskStatus;
import com.aurionpro.ticketboard.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "risks", indexes = {
    @Index(name = "idx_risk_project_id", columnList = "project_id"),
    @Index(name = "idx_risk_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Risk extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "risk_code", nullable = false, unique = true, length = 50)
    private String riskCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "probability", nullable = false)
    @Builder.Default
    private Integer probability = 3; // 1 to 5

    @Column(name = "impact", nullable = false)
    @Builder.Default
    private Integer impact = 3; // 1 to 5

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore; // probability * impact (1 to 25)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "mitigation_plan", columnDefinition = "TEXT")
    private String mitigationPlan;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private RiskStatus status = RiskStatus.IDENTIFIED;

    @PrePersist
    @PreUpdate
    public void calculateScore() {
        if (probability != null && impact != null) {
            this.riskScore = probability * impact;
        }
    }
}
