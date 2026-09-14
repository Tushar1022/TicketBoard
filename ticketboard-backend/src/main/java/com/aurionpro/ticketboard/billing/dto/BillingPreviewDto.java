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
public class BillingPreviewDto {
    private Long projectId;
    private String projectCode;
    private String projectName;
    private String clientName;
    private Double totalBillableHours;
    private Double totalAmount;
    private Double taxRate;
    private Double taxAmount;
    private Double grandTotal;
    private List<InvoiceLineItemDto> lineItems;
}