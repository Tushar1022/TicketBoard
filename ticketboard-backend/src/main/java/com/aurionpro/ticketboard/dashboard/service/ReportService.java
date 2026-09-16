package com.aurionpro.ticketboard.dashboard.service;

import com.aurionpro.ticketboard.dashboard.dto.ReportCatalogItemDto;
import com.aurionpro.ticketboard.dashboard.dto.ReportDataResponse;

import java.util.List;

public interface ReportService {

    ReportDataResponse getReportData(
            String category,
            Long projectId,
            String status,
            String startDate,
            String endDate
    );

    List<ReportCatalogItemDto> getReportCatalog();
}