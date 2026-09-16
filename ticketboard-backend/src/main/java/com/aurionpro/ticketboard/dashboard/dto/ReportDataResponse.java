package com.aurionpro.ticketboard.dashboard.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportDataResponse {
    private String reportCategory;
    private String title;
    private String subtitle;
    private List<Map<String, Object>> summaryKpis;
    private List<String> columns;
    private List<Map<String, Object>> rows;
    private long totalRecords;
    private List<SeriesPointDto> reportSeries;
    private String chartType;
    private String orientation;
}
