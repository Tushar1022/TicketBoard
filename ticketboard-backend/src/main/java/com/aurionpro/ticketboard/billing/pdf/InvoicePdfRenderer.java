package com.aurionpro.ticketboard.billing.pdf;

import com.aurionpro.ticketboard.billing.entity.Invoice;
import com.aurionpro.ticketboard.billing.entity.InvoiceLineItem;
import com.aurionpro.ticketboard.billing.enums.InvoiceStatus;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InvoicePdfRenderer {

    private static final Color INDIGO = new Color(79, 70, 229);
    private static final Color INDIGO_DEEP = new Color(55, 48, 163);
    private static final Color INDIGO_DARK = new Color(30, 27, 75);
    private static final Color SKY = new Color(2, 132, 199);
    private static final Color TEAL = new Color(13, 148, 136);
    private static final Color INK = new Color(30, 41, 59);
    private static final Color SLATE = new Color(71, 85, 105);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color FAINT = new Color(148, 163, 184);
    private static final Color PAPER = new Color(248, 250, 252);
    private static final Color LINE = new Color(226, 232, 240);
    private static final Color INDIGO_SOFT = new Color(238, 242, 255);
    private static final Color GREEN = new Color(5, 150, 105);
    private static final Color GREEN_SOFT = new Color(236, 253, 245);
    private static final Color AMBER = new Color(217, 119, 6);
    private static final Color AMBER_SOFT = new Color(255, 251, 235);
    private static final Color RED = new Color(220, 38, 38);
    private static final Color RED_SOFT = new Color(254, 242, 242);
    private static final Color SKY_SOFT = new Color(224, 242, 254);
    private static final Color WHITE = Color.WHITE;

    private static final BaseFont F_REG;
    private static final BaseFont F_MED;
    private static final BaseFont F_SEMI;
    private static final BaseFont F_BOLD;
    private static final BaseFont F_MONO;

    private static final byte[] LOGO_BYTES = buildLogo();
    private static final Map<String, byte[]> STAMP_CACHE = new ConcurrentHashMap<>();

    static {
        F_REG = loadFont("/static/fonts/Poppins-Regular.ttf");
        F_MED = loadFont("/static/fonts/Poppins-Medium.ttf");
        F_SEMI = loadFont("/static/fonts/Poppins-SemiBold.ttf");
        F_BOLD = loadFont("/static/fonts/Poppins-Bold.ttf");
        F_MONO = loadFont("/static/fonts/IBMPlexMono-Regular.ttf");
    }

    private InvoicePdfRenderer() {
    }

    public static byte[] render(Invoice invoice) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 30f, 30f, 24f, 46f);
        PdfWriter writer = PdfWriter.getInstance(document, out);
        writer.setPageEvent(new FooterPageEvent());
        document.open();

        drawBackground(writer);

        document.add(buildHeader(invoice));
        document.add(buildAccentStrip());
        document.add(spacer(8f));

        document.add(buildMeta(invoice));
        document.add(spacer(8f));

        document.add(buildParties(invoice));
        document.add(spacer(8f));

        document.add(buildItemsHeader());
        document.add(buildItems(invoice));
        document.add(spacer(8f));

        document.add(buildBottom(invoice));
        document.add(Chunk.NEWLINE);

        document.add(buildSignatures(invoice));

        addStamp(document, invoice);

        document.close();
        return out.toByteArray();
    }

    private static void drawBackground(PdfWriter writer) {
        PdfContentByte cb = writer.getDirectContentUnder();
        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                new Phrase("TICKETBOARD", font(F_BOLD, 64f, Font.NORMAL, new Color(236, 240, 250))),
                421f, 285f, -28f);
        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                new Phrase("Flagship Delivery Platform", font(F_SEMI, 16f, Font.NORMAL, new Color(241, 244, 250))),
                421f, 220f, -28f);
    }

    private static PdfPTable buildHeader(Invoice invoice) {
        PdfPTable header = new PdfPTable(new float[]{168f, 342f, 242f});
        header.setWidthPercentage(100f);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setBackgroundColor(INDIGO_DEEP);
        logoCell.setPaddingLeft(14f);
        logoCell.setPaddingTop(10f);
        logoCell.setPaddingBottom(10f);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        try {
            Image logo = Image.getInstance(LOGO_BYTES);
            logo.scaleToFit(48f, 48f);
            logoCell.addElement(logo);
        } catch (Exception ignored) {
        }
        header.addCell(logoCell);

        PdfPCell brandCell = new PdfPCell();
        brandCell.setBorder(Rectangle.NO_BORDER);
        brandCell.setBackgroundColor(INDIGO_DEEP);
        brandCell.setPaddingLeft(4f);
        brandCell.setPaddingTop(10f);
        brandCell.setPaddingBottom(8f);
        brandCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        brandCell.addElement(new Paragraph("TicketBoard", font(F_BOLD, 18f, Font.NORMAL, WHITE)));
        Paragraph tag = new Paragraph("Flagship Project Delivery & Work Management Platform  •  by Tushar",
                font(F_REG, 7.5f, Font.NORMAL, new Color(199, 210, 254)));
        tag.setLeading(11f);
        brandCell.addElement(tag);
        header.addCell(brandCell);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setBackgroundColor(INDIGO);
        titleCell.setPaddingRight(14f);
        titleCell.setPaddingTop(9f);
        titleCell.setPaddingBottom(8f);
        titleCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph title = new Paragraph("INVOICE", font(F_BOLD, 24f, Font.NORMAL, WHITE));
        title.setAlignment(Element.ALIGN_RIGHT);
        titleCell.addElement(title);
        Paragraph num = new Paragraph(safe(invoice.getInvoiceNumber()), font(F_SEMI, 10f, Font.NORMAL, new Color(221, 214, 254)));
        num.setAlignment(Element.ALIGN_RIGHT);
        titleCell.addElement(num);
        header.addCell(titleCell);

        return header;
    }

    private static PdfPTable buildAccentStrip() {
        PdfPTable strip = new PdfPTable(new float[]{620f, 132f});
        strip.setWidthPercentage(100f);
        PdfPCell a = new PdfPCell();
        a.setBorder(Rectangle.NO_BORDER);
        a.setFixedHeight(7f);
        a.setBackgroundColor(INDIGO);
        strip.addCell(a);
        PdfPCell b = new PdfPCell();
        b.setBorder(Rectangle.NO_BORDER);
        b.setFixedHeight(7f);
        b.setBackgroundColor(SKY);
        strip.addCell(b);
        return strip;
    }

    private static PdfPTable buildMeta(Invoice invoice) {
        PdfPTable meta = new PdfPTable(4);
        meta.setWidthPercentage(100f);
        meta.setWidths(new float[]{1f, 1f, 1f, 1f});
        addMetaCell(meta, "Invoice No", safe(invoice.getInvoiceNumber()), F_MONO, INK);
        addMetaCell(meta, "Billing Period", invoice.getFromDate() + "  to  " + invoice.getToDate(), F_REG, INK);
        addMetaCell(meta, "Issue Date", String.valueOf(invoice.getIssuedDate()), F_REG, INK);
        addMetaCell(meta, "Due Date", invoice.getDueDate() != null ? String.valueOf(invoice.getDueDate()) : "-", F_REG, INK);
        addMetaCell(meta, "Status", String.valueOf(invoice.getStatus()), F_BOLD, statusColor(invoice.getStatus()));
        addMetaCell(meta, "Payment Terms", "Net " + netDays(invoice), F_REG, INK);
        addMetaCell(meta, "PO / Ref", safe(invoice.getInvoiceNumber()), F_MONO, SLATE);
        addMetaCell(meta, "Currency", safe(invoice.getCurrency()), F_SEMI, INDIGO);
        return meta;
    }

    private static PdfPTable buildParties(Invoice invoice) {
        PdfPTable parties = new PdfPTable(2);
        parties.setWidthPercentage(100f);
        parties.setWidths(new float[]{1f, 1f});

        String clientName = invoice.getClient() != null ? safe(invoice.getClient().getName()) : "-";
        String contact = invoice.getClient() != null ? invoice.getClient().getContactPerson() : null;
        String email = invoice.getClient() != null ? invoice.getClient().getEmail() : null;
        String address = invoice.getClient() != null ? invoice.getClient().getAddress() : null;

        PdfPCell to = boxedCell();
        to.addElement(partyHead("BILL TO"));
        to.addElement(partyLine(clientName, F_SEMI, 10f, INK));
        if (contact != null && !contact.isBlank()) {
            to.addElement(partyLine("Attn: " + contact, F_REG, 8.5f, SLATE));
        }
        if (email != null && !email.isBlank()) {
            to.addElement(partyLine(email, F_REG, 8.5f, SLATE));
        }
        to.addElement(partyLine(address != null && !address.isBlank() ? address : "-", F_REG, 8.5f, SLATE));
        if (email != null && !email.isBlank()) {
            to.addElement(partyLine("Client Tax ID: " + "TAX-" + invoice.getClient().getId(), F_MONO, 7.5f, MUTED));
        }
        parties.addCell(to);

        PdfPCell from = boxedCell();
        from.addElement(partyHead("PAYABLE TO"));
        from.addElement(partyLine("TicketBoard by Tushar", F_SEMI, 10f, INK));
        from.addElement(partyLine("AurionPro Solutions Pvt. Ltd., Pune 411045, India", F_REG, 8.5f, SLATE));
        from.addElement(partyLine("accounts@ticketboard.ai  |  +91 20 4567 8900", F_REG, 8.5f, SLATE));
        from.addElement(partyLine("GSTIN: 27AABCT1234F1Z8   PAN: AABCT1234F", F_MONO, 7.5f, MUTED));
        if (invoice.getProject() != null) {
            from.addElement(partyLine("Project: " + safe(invoice.getProject().getProjectCode()) + "  •  " + safe(invoice.getProject().getName()),
                    F_REG, 8.5f, TEAL));
        }
        parties.addCell(from);

        return parties;
    }

    private static PdfPCell boxedCell() {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.BOX);
        c.setBorderColor(LINE);
        c.setBorderWidth(0.8f);
        c.setPadding(0f);
        c.setPaddingBottom(6f);
        return c;
    }

    private static PdfPTable partyHead(String text) {
        PdfPTable head = new PdfPTable(1);
        head.setWidthPercentage(100f);
        Paragraph p = new Paragraph(text, font(F_BOLD, 8f, Font.NORMAL, INDIGO));
        p.setLeading(12f);
        PdfPCell c = new PdfPCell(p);
        c.setBorder(Rectangle.NO_BORDER);
        c.setBackgroundColor(INDIGO_SOFT);
        c.setPaddingLeft(10f);
        c.setPaddingTop(5f);
        c.setPaddingBottom(5f);
        head.addCell(c);
        return head;
    }

    private static Paragraph partyLine(String text, BaseFont bf, float size, Color color) {
        Paragraph p = new Paragraph(text, font(bf, size, Font.NORMAL, color));
        p.setLeading(size + 5f);
        p.setIndentationLeft(10f);
        p.setSpacingBefore(3f);
        p.setSpacingAfter(1f);
        return p;
    }

    private static PdfPTable buildItemsHeader() {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100f);
        PdfPCell c = new PdfPCell(new Phrase("LINE ITEMS  •  BILLABLE CONSULTING SERVICES", font(F_BOLD, 10f, Font.NORMAL, INDIGO_DEEP)));
        c.setBorder(Rectangle.NO_BORDER);
        c.setBackgroundColor(PAPER);
        c.setPaddingLeft(0f);
        c.setPaddingTop(2f);
        c.setPaddingBottom(2f);
        table.addCell(c);
        return table;
    }

    private static PdfPTable buildItems(Invoice invoice) {
        float[] widths = new float[]{6f, 16f, 29f, 16f, 10f, 8f, 8f, 8f};
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100f);
        table.setHeaderRows(1);

        addHeaderCell(table, "#");
        addHeaderCell(table, "Ticket");
        addHeaderCell(table, "Work Item");
        addHeaderCell(table, "Consultant");
        addHeaderCell(table, "Type");
        addHeaderCell(table, "Hours");
        addHeaderCell(table, "Rate");
        addHeaderCell(table, "Amount");

        if (invoice.getLineItems() == null || invoice.getLineItems().isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("No billable time entries were found in the billing period.",
                    font(F_REG, 8.5f, Font.NORMAL, MUTED)));
            empty.setColspan(8);
            empty.setBorder(Rectangle.BOX);
            empty.setBorderColor(LINE);
            empty.setBorderWidth(0.6f);
            empty.setPadding(10f);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(empty);
            return table;
        }

        int idx = 1;
        for (InvoiceLineItem item : invoice.getLineItems()) {
            boolean zebra = idx % 2 == 0;
            addBodyCell(table, String.valueOf(idx), zebra, F_MONO, FAINT, Element.ALIGN_CENTER);
            addBodyCell(table, safe(item.getWorkItemNumber()), zebra, F_MONO, SKY, Element.ALIGN_LEFT);
            addBodyCell(table, safe(firstNonBlank(item.getWorkItemTitle(), item.getDescription())), zebra, F_MED, INK, Element.ALIGN_LEFT);
            addBodyCell(table, safe(item.getConsultantName()), zebra, F_REG, SLATE, Element.ALIGN_LEFT);
            addBodyCell(table, safe(item.getBillingType()).toUpperCase(), zebra, F_REG, TEAL, Element.ALIGN_CENTER);
            addBodyCell(table, trim(item.getHours()), zebra, F_MONO, SLATE, Element.ALIGN_RIGHT);
            addBodyCell(table, formatMoney(invoice, item.getRate()), zebra, F_MONO, SLATE, Element.ALIGN_RIGHT);
            addBodyCell(table, formatMoney(invoice, item.getAmount()), zebra, F_SEMI, INK, Element.ALIGN_RIGHT);
            idx++;
        }
        return table;
    }

    private static void addHeaderCell(PdfPTable table, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, font(F_SEMI, 8f, Font.NORMAL, WHITE)));
        c.setBackgroundColor(INDIGO_DARK);
        c.setPaddingLeft(6f);
        c.setPaddingTop(5f);
        c.setPaddingBottom(5f);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(c);
    }

    private static void addBodyCell(PdfPTable table, String text, boolean zebra, BaseFont bf, Color color, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, font(bf, 8f, Font.NORMAL, color)));
        c.setBorderColor(LINE);
        c.setBorderWidth(0.6f);
        c.setPaddingLeft(6f);
        c.setPaddingTop(4f);
        c.setPaddingBottom(4f);
        c.setHorizontalAlignment(align);
        if (zebra) {
            c.setBackgroundColor(PAPER);
        }
        table.addCell(c);
    }

    private static PdfPTable buildBottom(Invoice invoice) {
        PdfPTable container = new PdfPTable(new float[]{56f, 44f});
        container.setWidthPercentage(100f);

        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.setPadding(0f);
        left.addElement(buildCard("AMOUNT IN WORDS", amountInWords(invoice), MUTED, 8.5f, null));
        left.addElement(spacer(6f));
        left.addElement(buildBankCard());
        left.addElement(spacer(6f));
        left.addElement(buildTermsCard(invoice));
        container.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setPadding(0f);
        right.addElement(buildTotals(invoice));
        container.addCell(right);

        return container;
    }

    private static PdfPTable buildCard(String label, String value, Color labelColor, float size, Object ignored) {
        PdfPTable card = new PdfPTable(1);
        card.setWidthPercentage(100f);

        PdfPCell head = new PdfPCell(new Phrase(label, font(F_BOLD, 7.5f, Font.NORMAL, INDIGO)));
        head.setBorder(Rectangle.BOX);
        head.setBorderColor(LINE);
        head.setBorderWidth(0.8f);
        head.setBackgroundColor(INDIGO_SOFT);
        head.setPaddingLeft(8f);
        head.setPaddingTop(4f);
        head.setPaddingBottom(4f);
        card.addCell(head);

        PdfPCell body = new PdfPCell(new Phrase(value, font(F_REG, size, Font.NORMAL, labelColor)));
        body.setBorder(Rectangle.BOX);
        body.setBorderColor(LINE);
        body.setBorderWidth(0.8f);
        body.setPaddingLeft(8f);
        body.setPaddingTop(5f);
        body.setPaddingBottom(5f);
        card.addCell(body);
        return card;
    }

    private static PdfPTable buildBankCard() {
        PdfPTable card = new PdfPTable(1);
        card.setWidthPercentage(100f);
        PdfPCell head = new PdfPCell(new Phrase("PAYMENT / BANK DETAILS", font(F_BOLD, 7.5f, Font.NORMAL, INDIGO)));
        head.setBorder(Rectangle.BOX);
        head.setBorderColor(LINE);
        head.setBorderWidth(0.8f);
        head.setBackgroundColor(INDIGO_SOFT);
        head.setPaddingLeft(8f);
        head.setPaddingTop(4f);
        head.setPaddingBottom(4f);
        card.addCell(head);

        PdfPTable grid = new PdfPTable(2);
        grid.setWidths(new float[]{38f, 62f});
        grid.setWidthPercentage(100f);
        addKv(grid, "Beneficiary", "TicketBoard by Tushar");
        addKv(grid, "Account No", "9845 2210 4410");
        addKv(grid, "Bank / Branch", "DBS Bank — Pune Main");
        addKv(grid, "IFSC Code", "DBSS0IN0811");
        addKv(grid, "SWIFT", "DBSSSGSG");
        addKv(grid, "GSTIN", "27AABCT1234F1Z8");

        PdfPCell body = new PdfPCell(grid);
        body.setBorder(Rectangle.BOX);
        body.setBorderColor(LINE);
        body.setBorderWidth(0.8f);
        body.setPadding(4f);
        card.addCell(body);
        return card;
    }

    private static PdfPTable buildTermsCard(Invoice invoice) {
        PdfPTable card = new PdfPTable(1);
        card.setWidthPercentage(100f);
        PdfPCell head = new PdfPCell(new Phrase("TERMS & CONDITIONS", font(F_BOLD, 7.5f, Font.NORMAL, INDIGO)));
        head.setBorder(Rectangle.BOX);
        head.setBorderColor(LINE);
        head.setBorderWidth(0.8f);
        head.setBackgroundColor(INDIGO_SOFT);
        head.setPaddingLeft(8f);
        head.setPaddingTop(4f);
        head.setPaddingBottom(4f);
        card.addCell(head);

        String notes = invoice.getNotes() != null && !invoice.getNotes().isBlank()
                ? invoice.getNotes()
                : "Payments are due within " + netDays(invoice) + " days of the issue date. "
                + "Invoices unpaid after the due date may accrue a 1.5% monthly late fee. "
                + "Please quote the invoice number in your remittance. "
                + "This invoice is generated electronically from approved billable time entries.";
        PdfPCell body = new PdfPCell(new Phrase(notes, font(F_REG, 8f, Font.NORMAL, SLATE)));
        body.setBorder(Rectangle.BOX);
        body.setBorderColor(LINE);
        body.setBorderWidth(0.8f);
        body.setPaddingLeft(8f);
        body.setPaddingTop(5f);
        body.setPaddingBottom(5f);
        card.addCell(body);
        return card;
    }

    private static void addKv(PdfPTable grid, String k, String v) {
        PdfPCell kc = new PdfPCell(new Phrase(k, font(F_SEMI, 7f, Font.NORMAL, FAINT)));
        kc.setBorderColor(LINE);
        kc.setBorderWidth(0.6f);
        kc.setBackgroundColor(PAPER);
        kc.setPaddingLeft(6f);
        kc.setPaddingTop(3f);
        kc.setPaddingBottom(3f);
        grid.addCell(kc);

        PdfPCell vc = new PdfPCell(new Phrase(v, font(F_MONO, 7.5f, Font.NORMAL, SLATE)));
        vc.setBorderColor(LINE);
        vc.setBorderWidth(0.6f);
        vc.setPaddingLeft(6f);
        vc.setPaddingTop(3f);
        vc.setPaddingBottom(3f);
        grid.addCell(vc);
    }

    private static PdfPTable buildTotals(Invoice invoice) {
        PdfPTable totals = new PdfPTable(2);
        totals.setWidthPercentage(100f);
        totals.setWidths(new float[]{1f, 1f});

        addTotalRow(totals, "TOTAL BILLABLE HOURS", trim(hoursOf(invoice)), F_REG, SLATE, PAPER);
        addTotalRow(totals, "SUBTOTAL", formatMoney(invoice, invoice.getSubtotal()), F_SEMI, INK, PAPER);
        addTotalRow(totals, "TAXABLE AMOUNT", formatMoney(invoice, invoice.getSubtotal()), F_REG, SLATE, PAPER);
        addTotalRow(totals, "TAX (" + trim(invoice.getTaxRate()) + "%)", formatMoney(invoice, invoice.getTaxAmount()), F_SEMI, TEAL, PAPER);

        PdfPCell grand = new PdfPCell(new Phrase("GRAND TOTAL", font(F_BOLD, 9f, Font.NORMAL, WHITE)));
        grand.setBackgroundColor(INDIGO_DEEP);
        grand.setPaddingLeft(8f);
        grand.setPaddingTop(6f);
        grand.setPaddingBottom(6f);
        grand.setHorizontalAlignment(Element.ALIGN_RIGHT);
        grand.setBorder(Rectangle.NO_BORDER);
        totals.addCell(grand);

        PdfPCell grandVal = new PdfPCell(new Phrase(formatMoney(invoice, invoice.getTotal()), font(F_BOLD, 11f, Font.NORMAL, WHITE)));
        grandVal.setBackgroundColor(INDIGO_DEEP);
        grandVal.setPaddingLeft(8f);
        grandVal.setPaddingTop(6f);
        grandVal.setPaddingBottom(6f);
        grandVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        grandVal.setBorder(Rectangle.NO_BORDER);
        totals.addCell(grandVal);

        if (invoice.getDueDate() != null) {
            PdfPCell due = new PdfPCell(new Phrase(
                    "Amount payable by " + invoice.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    font(F_SEMI, 7.5f, Font.NORMAL, AMBER)));
            due.setBorder(Rectangle.NO_BORDER);
            due.setBackgroundColor(AMBER_SOFT);
            due.setPaddingLeft(8f);
            due.setPaddingTop(5f);
            due.setPaddingBottom(5f);
            due.setHorizontalAlignment(Element.ALIGN_RIGHT);
            due.setColspan(2);
            totals.addCell(due);
        }
        return totals;
    }

    private static void addTotalRow(PdfPTable totals, String label, String value, BaseFont bf, Color color, Color bg) {
        PdfPCell lc = new PdfPCell(new Phrase(label, font(bf, 8f, Font.NORMAL, color)));
        lc.setBorderColor(LINE);
        lc.setBorderWidth(0.6f);
        lc.setBackgroundColor(bg);
        lc.setPaddingLeft(8f);
        lc.setPaddingTop(4f);
        lc.setPaddingBottom(4f);
        totals.addCell(lc);

        PdfPCell vc = new PdfPCell(new Phrase(value, font(F_SEMI, 8f, Font.NORMAL, color)));
        vc.setBorderColor(LINE);
        vc.setBorderWidth(0.6f);
        vc.setBackgroundColor(bg);
        vc.setPaddingLeft(8f);
        vc.setPaddingTop(4f);
        vc.setPaddingBottom(4f);
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totals.addCell(vc);
    }

    private static PdfPTable buildSignatures(Invoice invoice) {
        PdfPTable sig = new PdfPTable(3);
        sig.setWidthPercentage(100f);
        sig.setWidths(new float[]{1f, 1f, 1f});
        addSignatureCell(sig, "PREPARED BY", invoice.getCreatedByUser() != null ? invoice.getCreatedByUser().getFullName() : "Accounts Team");
        addSignatureCell(sig, "APPROVED BY", "Finance Controller");
        addSignatureCell(sig, "RECEIVED BY", "Authorised Signatory");
        return sig;
    }

    private static void addSignatureCell(PdfPTable sig, String label, String name) {
        PdfPTable inner = new PdfPTable(1);
        inner.setWidthPercentage(100f);
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setFixedHeight(52f);
        inner.addCell(c);
        PdfPCell line = new PdfPCell();
        line.setBorder(Rectangle.BOTTOM);
        line.setBorderColor(SLATE);
        line.setBorderWidth(0.6f);
        line.setFixedHeight(16f);
        line.setPaddingLeft(4f);
        inner.addCell(line);
        PdfPCell meta = new PdfPCell(new Phrase(label + "  —  " + name, font(F_SEMI, 7f, Font.NORMAL, MUTED)));
        meta.setBorder(Rectangle.NO_BORDER);
        meta.setPaddingTop(2f);
        meta.setPaddingLeft(4f);
        inner.addCell(meta);
        sig.addCell(inner);
    }

    private static void addStamp(Document document, Invoice invoice) {
        try {
            byte[] bytes = stampFor(invoice.getStatus());
            Image stamp = Image.getInstance(bytes);
            stamp.setRotationDegrees(-14f);
            float w = 260f;
            float h = w * stamp.getScaledHeight() / stamp.getScaledWidth();
            stamp.scaleAbsolute(w, h);
            stamp.setAbsolutePosition((PageSize.A4.getWidth() - w) / 2f, (PageSize.A4.getHeight() - h) / 2f + 20f);
            document.add(stamp);
        } catch (Exception ignored) {
        }
    }

    private static PdfPTable spacer(float h) {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100f);
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setFixedHeight(h);
        t.addCell(c);
        return t;
    }

    private static void addMetaCell(PdfPTable meta, String label, String value, BaseFont bf, Color color) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.BOX);
        c.setBorderColor(LINE);
        c.setBorderWidth(0.7f);
        PageBackground(c);
        c.setPaddingLeft(8f);
        c.setPaddingTop(4f);
        c.setPaddingBottom(4f);
        Paragraph l = new Paragraph(label.toUpperCase(), font(F_BOLD, 6.5f, Font.NORMAL, FAINT));
        l.setLeading(8f);
        c.addElement(l);
        Paragraph v = new Paragraph(value, font(bf, 8.5f, Font.NORMAL, color));
        v.setLeading(11f);
        c.addElement(v);
        meta.addCell(c);
    }

    private static void PageBackground(PdfPCell c) {
        c.setBackgroundColor(PAPER);
    }

    private static final class FooterPageEvent extends PdfPageEventHelper {
        private PdfTemplate total;
        private int pageNumber = 0;

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            total = writer.getDirectContent().createTemplate(34f, 14f);
            try {
                total.setFontAndSize(F_MED, 7.5f);
                total.setTextMatrix(0f, 0f);
                total.showText("0");
            } catch (Exception ignored) {
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            pageNumber++;
            PdfContentByte cb = writer.getDirectContent();
            cb.setColorStroke(LINE);
            cb.moveTo(30f, 34f);
            cb.lineTo(PageSize.A4.getWidth() - 30f, 34f);
            cb.stroke();
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase("TicketBoard by Tushar  •  Flagship Project Delivery Platform  •  Generated "
                            + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                            font(F_MED, 7f, Font.NORMAL, FAINT)),
                    30f, 20f, 0f);
            cb.saveState();
            cb.concatCTM(1f, 0f, 0f, 1f, PageSize.A4.getWidth() - 58f, 20f);
            cb.beginText();
            cb.setFontAndSize(F_MED, 7.5f);
            cb.setTextMatrix(0f, 0f);
            cb.showText("Page " + pageNumber + " / ");
            cb.endText();
            cb.addTemplate(total, 0f, 0f);
            cb.restoreState();
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            total.beginText();
            total.setFontAndSize(F_MED, 7.5f);
            total.setTextMatrix(0f, 0f);
            total.showText(String.valueOf(pageNumber));
            total.endText();
        }
    }

    private static BaseFont loadFont(String path) {
        try {
            return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (Exception e) {
            try {
                return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            } catch (Exception ex) {
                throw new IllegalStateException("No usable base font available", ex);
            }
        }
    }

    private static Font font(BaseFont bf, float size, int style, Color color) {
        return new Font(bf, size, style, color);
    }

    private static String safe(String s) {
        return s != null && !s.isBlank() ? s : "-";
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private static String trim(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) {
            return String.format(Locale.US, "%.0f", v);
        }
        return String.format(Locale.US, "%.2f", v);
    }

    private static double hoursOf(Invoice invoice) {
        if (invoice.getLineItems() == null) {
            return 0;
        }
        return invoice.getLineItems().stream().mapToDouble(InvoiceLineItem::getHours).sum();
    }

    private static String formatMoney(Invoice invoice, double value) {
        return currencySymbol(safe(invoice.getCurrency())) + String.format(Locale.US, "%,.2f", value);
    }

    private static String currencySymbol(String currency) {
        if ("INR".equalsIgnoreCase(currency)) {
            return "\u20B9";
        }
        if ("EUR".equalsIgnoreCase(currency)) {
            return "\u20AC";
        }
        if ("GBP".equalsIgnoreCase(currency)) {
            return "\u00A3";
        }
        if ("JPY".equalsIgnoreCase(currency)) {
            return "\u00A5";
        }
        return "$";
    }

    private static int netDays(Invoice invoice) {
        if (invoice.getDueDate() != null && invoice.getIssuedDate() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(invoice.getIssuedDate(), invoice.getDueDate());
            if (days >= 0) {
                return (int) days;
            }
        }
        return 30;
    }

    private static Color statusColor(InvoiceStatus status) {
        if (status == null) {
            return SLATE;
        }
        switch (status) {
            case PAID:
                return GREEN;
            case OVERDUE:
                return RED;
            case SENT:
                return SKY;
            case CANCELLED:
                return RED;
            default:
                return SLATE;
        }
    }

    private static String amountInWords(Invoice invoice) {
        double v = invoice.getTotal() != null ? invoice.getTotal() : 0;
        long whole = (long) Math.floor(v);
        long cents = Math.round((v - whole) * 100.0);
        String unit = "INR".equalsIgnoreCase(safe(invoice.getCurrency())) ? "Rupees" : "Dollars";
        String small = "INR".equalsIgnoreCase(safe(invoice.getCurrency())) ? "Paisa" : "Cents";
        StringBuilder sb = new StringBuilder();
        if (whole == 0 && cents == 0) {
            sb.append("Zero ").append(unit.toLowerCase()).append(" only");
        } else {
            if (whole > 0) {
                sb.append(numberToWords(whole)).append(" ").append(unit).append(" ");
            }
            if (cents > 0) {
                if (whole > 0) {
                    sb.append("and ");
                }
                sb.append(numberToWords(cents)).append(" ").append(small).append(" ");
            }
            sb.append("only.");
        }
        return sb.toString();
    }

    private static final String[] ONES = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
    private static final String[] TENS = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};

    private static String numberToWords(long n) {
        if (n == 0) {
            return "Zero";
        }
        if (n < 0) {
            return "Minus " + numberToWords(-n);
        }
        if (n < 20) {
            return ONES[(int) n];
        }
        if (n < 100) {
            return TENS[(int) (n / 10)] + (n % 10 != 0 ? " " + ONES[(int) (n % 10)] : "");
        }
        if (n < 1000) {
            return ONES[(int) (n / 100)] + " Hundred" + (n % 100 != 0 ? " " + numberToWords(n % 100) : "");
        }
        if (n < 1_000_000) {
            return numberToWords(n / 1000) + " Thousand" + (n % 1000 != 0 ? " " + numberToWords(n % 1000) : "");
        }
        if (n < 1_000_000_000L) {
            return numberToWords(n / 1_000_000) + " Million" + (n % 1_000_000 != 0 ? " " + numberToWords(n % 1_000_000) : "");
        }
        return numberToWords(n / 1_000_000_000L) + " Billion" + (n % 1_000_000_000L != 0 ? " " + numberToWords(n % 1_000_000_000L) : "");
    }

    private static byte[] buildLogo() {
        BufferedImage img = new BufferedImage(240, 240, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        RoundRectangle2D tile = new RoundRectangle2D.Float(6f, 6f, 228f, 228f, 56f, 56f);
        g.setPaint(new java.awt.GradientPaint(6f, 6f, new Color(99, 102, 241), 228f, 228f, new Color(2, 132, 199)));
        g.fill(tile);

        g.setColor(new Color(255, 255, 255, 60));
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new RoundRectangle2D.Float(16f, 16f, 208f, 208f, 48f, 48f));

        g.setColor(WHITE);
        g.setStroke(new BasicStroke(22f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int[] xs = {68, 110, 176};
        int[] ys = {128, 168, 88};
        g.drawPolyline(xs, ys, 3);

        g.setColor(new Color(79, 70, 229));
        g.fill(new Ellipse2D.Float(158f, 44f, 34f, 34f));
        g.setColor(WHITE);
        g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 20));
        g.drawString("TB", 164f, 69f);

        g.dispose();
        return toPng(img);
    }

    private static byte[] stampFor(InvoiceStatus status) {
        String key = String.valueOf(status);
        return STAMP_CACHE.computeIfAbsent(key, k -> buildStamp(status));
    }

    private static byte[] buildStamp(InvoiceStatus status) {
        Color color = statusColor(status == null ? InvoiceStatus.DRAFT : status);
        String text = status == null ? "DRAFT" : status.name();
        int w = 520;
        int h = 176;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 205));
        g.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Ellipse2D.Float(6f, 6f, w - 12f, h - 12f));

        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 150));
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f, new float[]{14f, 10f}, 0f));
        g.draw(new Ellipse2D.Float(26f, 26f, w - 52f, h - 52f));

        java.awt.Font f = new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 72);
        try {
            java.awt.Font poppins = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT,
                    InvoicePdfRenderer.class.getResourceAsStream("/static/fonts/Poppins-Bold.ttf"));
            f = poppins.deriveFont(java.awt.Font.BOLD, 66f);
        } catch (FontFormatException | IOException ignored) {
        }
        g.setFont(f);
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 245));
        java.awt.FontMetrics fm = g.getFontMetrics();
        float tw = fm.stringWidth(text);
        float x = (w - tw) / 2f;
        float y = (h - fm.getHeight()) / 2f + fm.getAscent();
        g.drawString(text, x, y);

        g.dispose();
        return toPng(img);
    }

    private static byte[] toPng(BufferedImage img) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", bos);
            return bos.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }
}
