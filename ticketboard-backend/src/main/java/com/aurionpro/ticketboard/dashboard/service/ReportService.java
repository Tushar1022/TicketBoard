package com.aurionpro.ticketboard.dashboard.service;

import com.aurionpro.ticketboard.dashboard.dto.ReportDataResponse;

public interface ReportService {

    ReportDataResponse getReportData(
            String category,
            Long projectId,
            String status,
            String startDate,
            String endDate
    );
}
