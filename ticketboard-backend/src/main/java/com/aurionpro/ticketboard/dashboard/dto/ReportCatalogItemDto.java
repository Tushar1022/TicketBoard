package com.aurionpro.ticketboard.dashboard.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportCatalogItemDto {
    private String category;
    private String title;
    private String subtitle;
    private String chartType;
    private String orientation;
    private List<String> summaryLabels;
    private List<ReportColumnDefDto> columns;
}