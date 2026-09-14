package com.aurionpro.ticketboard.billing.controller;

import com.aurionpro.ticketboard.billing.dto.*;
import com.aurionpro.ticketboard.billing.enums.InvoiceStatus;
import com.aurionpro.ticketboard.billing.service.BillingService;
import com.aurionpro.ticketboard.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @PreAuthorize("hasAnyAuthority('billing:view', 'billing:manage')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<InvoiceDto>>> getAllInvoices(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) InvoiceStatus status) {
        // For now fetch all; filtering can be added later
        List<InvoiceDto> invoices = billingService.getAllInvoices(projectId, clientId, status);
        return ResponseEntity.ok(ApiResponse.ok("Invoices fetched successfully", invoices));
    }

    @PreAuthorize("hasAnyAuthority('billing:view', 'billing:manage')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceDto>> getInvoice(@PathVariable Long id) {
        InvoiceDto invoice = billingService.getInvoiceById(id);
        return ResponseEntity.ok(ApiResponse.ok("Invoice fetched successfully", invoice));
    }

    @PreAuthorize("hasAnyAuthority('billing:view', 'billing:manage')")
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long id) {
        byte[] pdf = billingService.generateInvoicePdf(id);
        return ResponseEntity.ok()
                .header("Content-Disposition", ContentDisposition.attachment()
                        .filename("Invoice.pdf", StandardCharsets.UTF_8)
                        .build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PreAuthorize("hasAuthority('billing:manage')")
    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceDto>> createInvoice(@Valid @RequestBody InvoiceCreateDto dto) {
        InvoiceDto invoice = billingService.createInvoice(dto);
        return ResponseEntity.ok(ApiResponse.ok("Invoice created successfully", invoice));
    }

    @PreAuthorize("hasAuthority('billing:manage')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceDto>> updateInvoice(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, Object> updates) {
        String notes = updates != null && updates.containsKey("notes") ? (String) updates.get("notes") : null;
        String dueDateStr = updates != null && updates.containsKey("dueDate") ? (String) updates.get("dueDate") : null;
        Double taxRate = updates != null && updates.containsKey("taxRate") ? ((Number) updates.get("taxRate")).doubleValue() : null;
        LocalDate dueDate = dueDateStr != null ? LocalDate.parse(dueDateStr) : null;
        InvoiceDto invoice = billingService.updateInvoice(id, notes, dueDate, taxRate);
        return ResponseEntity.ok(ApiResponse.ok("Invoice updated successfully", invoice));
    }

    @PreAuthorize("hasAuthority('billing:manage')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<InvoiceDto>> updateStatus(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        InvoiceStatus status = InvoiceStatus.valueOf(body.get("status"));
        InvoiceDto invoice = billingService.updateInvoiceStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Invoice status updated successfully", invoice));
    }

    @PreAuthorize("hasAuthority('billing:manage')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInvoice(@PathVariable Long id) {
        billingService.deleteInvoice(id);
        return ResponseEntity.ok(ApiResponse.ok("Invoice deleted successfully", null));
    }

    @PreAuthorize("hasAnyAuthority('billing:view', 'billing:manage')")
    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<BillingPreviewDto>> previewBilling(
            @RequestParam Long projectId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        BillingPreviewDto preview = billingService.previewBilling(projectId, fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.ok("Billing preview generated", preview));
    }

    @PreAuthorize("hasAnyAuthority('billing:view', 'billing:manage')")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<BillingSummaryDto>> getBillingSummary() {
        BillingSummaryDto summary = billingService.getBillingSummary();
        return ResponseEntity.ok(ApiResponse.ok("Billing summary fetched successfully", summary));
    }
}