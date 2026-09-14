package com.aurionpro.ticketboard.support.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketCommentDto {
    private Long id;
    private Long ticketId;
    private Long authorId;
    private String authorName;
    private String authorRole;
    private String commentText;
    private LocalDateTime createdAt;
}
