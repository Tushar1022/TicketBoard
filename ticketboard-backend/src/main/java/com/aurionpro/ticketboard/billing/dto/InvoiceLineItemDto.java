package com.aurionpro.ticketboard.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceLineItemDto {
    private Long id;
    private Long workItemId;
    private String workItemNumber;
    private String workItemTitle;
    private Long consultantId;
    private String consultantName;
    private String billingType;
    private String description;
    private Double hours;
    private Double rate;
    private Double amount;
}