package com.aurionpro.ticketboard.dashboard.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportColumnDefDto {
    private String key;
    private String label;
    private String align;
    private String format;
}