package com.aurionpro.ticketboard.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueWatcherDto {
    private Long userId;
    private String userName;
    private LocalDateTime addedAt;
}