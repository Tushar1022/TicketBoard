package com.aurionpro.ticketboard.project.entity;

import com.aurionpro.ticketboard.common.entity.BaseEntity;
import com.aurionpro.ticketboard.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "milestone_notes", indexes = {
    @Index(name = "idx_milestone_note_milestone_id", columnList = "milestone_id"),
    @Index(name = "idx_milestone_note_author_id", columnList = "author_id"),
    @Index(name = "idx_milestone_note_pinned", columnList = "pinned")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilestoneNote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_id", nullable = false)
    private Milestone milestone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "pinned", nullable = false)
    @Builder.Default
    private Boolean pinned = false;
}