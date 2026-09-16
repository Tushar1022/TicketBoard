package com.aurionpro.ticketboard.dashboard.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeriesPointDto {
    private String label;
    private Long count;
}