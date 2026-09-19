package com.aurionpro.ticketboard.risk.entity;

import com.aurionpro.ticketboard.common.entity.BaseEntity;
import com.aurionpro.ticketboard.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "issue_history", indexes = {
    @Index(name = "idx_issue_history_issue_id", columnList = "issue_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(name = "actor_name", length = 150)
    private String actorName;

    @Column(name = "action_type", nullable = false, length = 40)
    private String actionType;

    @Column(name = "field_name", length = 50)
    private String fieldName;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    protected void onPersist() {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }
}