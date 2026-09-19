package com.aurionpro.ticketboard.risk.entity;

import com.aurionpro.ticketboard.common.entity.BaseEntity;
import com.aurionpro.ticketboard.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "issue_comments", indexes = {
    @Index(name = "idx_issue_comment_issue_id", columnList = "issue_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
}