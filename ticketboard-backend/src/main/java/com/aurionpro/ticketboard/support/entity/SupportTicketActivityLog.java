package com.aurionpro.ticketboard.support.entity;

import com.aurionpro.ticketboard.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Immutable, append-only activity record for a support ticket.
 * Written on every state transition / assignment / comment / attachment
 * so the UI can render a real resolved-history timeline (status, priority,
 * assignee, category changes and who/colleague acted).
 */
@Entity
@Table(name = "support_ticket_activity_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicketActivityLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "actor_name", length = 150)
    private String actorName;

    @Column(name = "actor_role", length = 50)
    private String actorRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_before", length = 30)
    private TicketStatus statusBefore;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_after", length = 30)
    private TicketStatus statusAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_before", length = 20)
    private TicketPriority priorityBefore;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_after", length = 20)
    private TicketPriority priorityAfter;

    @Column(name = "assigned_to_before", length = 150)
    private String assignedToBefore;

    @Column(name = "assigned_to_after", length = 150)
    private String assignedToAfter;

    @Column(name = "action_type", nullable = false, length = 40)
    private String actionType;

    @Column(name = "summary", length = 255)
    private String summary;

    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    protected void onPersist() {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }
}
