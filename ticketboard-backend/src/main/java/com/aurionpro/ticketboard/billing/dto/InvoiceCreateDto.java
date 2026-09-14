package com.aurionpro.ticketboard.billing.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceCreateDto {

    @NotNull
    private Long projectId;

    @NotNull
    private LocalDate fromDate;

    @NotNull
    private LocalDate toDate;

    private Double taxRate;

    private String currency;

    private String notes;

    private LocalDate issuedDate;

    private LocalDate dueDate;
}