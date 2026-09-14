package com.aurionpro.ticketboard.billing.dto;

import com.aurionpro.ticketboard.billing.enums.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDto {
    private Long id;
    private String invoiceNumber;
    private Long clientId;
    private String clientName;
    private String clientContactPerson;
    private String clientEmail;
    private String clientAddress;
    private Long projectId;
    private String projectCode;
    private String projectName;
    private LocalDate fromDate;
    private LocalDate toDate;
    private LocalDate issuedDate;
    private LocalDate dueDate;
    private InvoiceStatus status;
    private Double subtotal;
    private Double taxRate;
    private Double taxAmount;
    private Double total;
    private String currency;
    private String notes;
    private String createdByName;
    private List<InvoiceLineItemDto> lineItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}