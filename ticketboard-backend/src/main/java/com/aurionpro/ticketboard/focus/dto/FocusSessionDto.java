package com.aurionpro.ticketboard.focus.dto;

import com.aurionpro.ticketboard.focus.entity.FocusSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FocusSessionDto {
    private Long id;
    private Long userId;
    private Long workItemId;
    private String workItemTicketNumber;
    private String workItemTitle;
    private Long projectId;
    private FocusSessionStatus status;
    private long elapsedSeconds;
    private long accumulatedSeconds;
    private LocalDateTime startedAt;
    private String description;
}