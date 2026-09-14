package com.aurionpro.ticketboard.billing.pdf;

import com.aurionpro.ticketboard.billing.entity.Invoice;
import com.aurionpro.ticketboard.billing.entity.InvoiceLineItem;
import com.aurionpro.ticketboard.billing.enums.InvoiceStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoicePdfRendererTest {

    @Test
    void rendersBrandedLandscapeA4Invoice() throws Exception {
        InvoiceLineItem line = InvoiceLineItem.builder()
                .workItemNumber("W-2401")
                .workItemTitle("Landing page redesign and responsive build")
                .consultantName("Tushar Shinde")
                .billingType("Billable")
                .description("UI/UX implementation")
                .hours(42.5)
                .rate(55.0)
                .amount(2337.5)
                .build();

        Invoice invoice = Invoice.builder()
                .invoiceNumber("INV-2026-001")
                .fromDate(LocalDate.of(2026, 8, 1))
                .toDate(LocalDate.of(2026, 8, 31))
                .issuedDate(LocalDate.of(2026, 9, 1))
                .dueDate(LocalDate.of(2026, 10, 1))
                .status(InvoiceStatus.SENT)
                .subtotal(2337.50)
                .taxRate(18.0)
                .taxAmount(420.75)
                .total(2758.25)
                .currency("USD")
                .notes("Payment due within Net 30.")
                .lineItems(Arrays.asList(line))
                .build();

        byte[] pdf = InvoicePdfRenderer.render(invoice);

        java.nio.file.Files.write(java.nio.file.Paths.get("target", "sample-invoice.pdf"), pdf);

        assertTrue(pdf.length > 2000, "PDF should be a substantial file");
        assertArrayEquals(new byte[]{'%', 'P', 'D', 'F', '-'}, Arrays.copyOfRange(pdf, 0, 5));
    }
}