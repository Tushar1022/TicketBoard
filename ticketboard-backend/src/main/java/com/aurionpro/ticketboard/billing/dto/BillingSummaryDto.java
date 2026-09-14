package com.aurionpro.ticketboard.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingSummaryDto {
    private Double totalRevenueBilled;
    private Double totalRevenuePaid;
    private Double totalOutstanding;
    private long invoiceCount;
    private long paidInvoiceCount;
    private long pendingInvoiceCount;
    private List<ProjectBillingSummaryDto> projects;
}