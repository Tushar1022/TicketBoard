package com.aurionpro.ticketboard.billing.service;

import com.aurionpro.ticketboard.audit.enums.TimelineEventType;
import com.aurionpro.ticketboard.audit.service.ActivityLogService;
import com.aurionpro.ticketboard.billing.dto.*;
import com.aurionpro.ticketboard.billing.entity.Invoice;
import com.aurionpro.ticketboard.billing.entity.InvoiceLineItem;
import com.aurionpro.ticketboard.billing.enums.InvoiceStatus;
import com.aurionpro.ticketboard.billing.pdf.InvoicePdfRenderer;
import com.aurionpro.ticketboard.billing.repository.InvoiceRepository;
import com.aurionpro.ticketboard.client.entity.Client;
import com.aurionpro.ticketboard.common.exception.BadRequestException;
import com.aurionpro.ticketboard.common.exception.ResourceNotFoundException;
import com.aurionpro.ticketboard.project.entity.Project;
import com.aurionpro.ticketboard.project.repository.ProjectRepository;
import com.aurionpro.ticketboard.timetracking.entity.TimeEntry;
import com.aurionpro.ticketboard.timetracking.enums.TimeEntryStatus;
import com.aurionpro.ticketboard.timetracking.repository.TimeEntryRepository;
import com.aurionpro.ticketboard.user.entity.User;
import com.aurionpro.ticketboard.user.repository.UserRepository;
import com.aurionpro.ticketboard.workitem.entity.WorkItem;
import com.aurionpro.ticketboard.workitem.repository.WorkItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final InvoiceRepository invoiceRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final WorkItemRepository workItemRepository;
    private final ActivityLogService activityLogService;

    @Transactional
    public InvoiceDto createInvoice(InvoiceCreateDto dto) {
        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", dto.getProjectId()));

        User currentUser = getCurrentUser();

        String invoiceNumber = generateInvoiceNumber();
        List<InvoiceLineItem> lineItems = computeLineItems(project, dto.getFromDate(), dto.getToDate());
        double subtotal = lineItems.stream().mapToDouble(InvoiceLineItem::getAmount).sum();
        double taxRate = dto.getTaxRate() != null ? dto.getTaxRate() : 18.0;
        double taxAmount = Math.round(subtotal * taxRate / 100.0 * 100.0) / 100.0;
        double total = Math.round((subtotal + taxAmount) * 100.0) / 100.0;

        Client client = project.getClient();

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .project(project)
                .client(client)
                .fromDate(dto.getFromDate())
                .toDate(dto.getToDate())
                .issuedDate(dto.getIssuedDate() != null ? dto.getIssuedDate() : LocalDate.now())
                .dueDate(dto.getDueDate())
                .status(InvoiceStatus.DRAFT)
                .subtotal(Math.round(subtotal * 100.0) / 100.0)
                .taxRate(taxRate)
                .taxAmount(taxAmount)
                .total(total)
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "USD")
                .notes(dto.getNotes())
                .createdByUser(currentUser)
                .build();

        lineItems.forEach(item -> item.setInvoice(invoice));
        invoice.setLineItems(lineItems);

        Invoice saved = invoiceRepository.save(invoice);

        activityLogService.logEvent(
                "PROJECT",
                project.getId(),
                project.getProjectCode(),
                TimelineEventType.STATUS_CHANGED,
                "Invoice " + invoiceNumber + " created for project " + project.getProjectCode() + " [" + dto.getFromDate() + " to " + dto.getToDate() + "] - Total: $" + total,
                "Invoice generated",
                null,
                invoiceNumber
        );

        return mapToDto(saved);
    }

    @Transactional
    public InvoiceDto updateInvoiceStatus(Long id, InvoiceStatus newStatus) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));
        invoice.setStatus(newStatus);
        Invoice saved = invoiceRepository.save(invoice);
        return mapToDto(saved);
    }

    @Transactional
    public InvoiceDto updateInvoice(Long id, String notes, LocalDate dueDate, Double taxRate) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));
        if (notes != null) invoice.setNotes(notes);
        if (dueDate != null) invoice.setDueDate(dueDate);
        if (taxRate != null) {
            invoice.setTaxRate(taxRate);
            double taxAmount = Math.round(invoice.getSubtotal() * taxRate / 100.0 * 100.0) / 100.0;
            invoice.setTaxAmount(taxAmount);
            invoice.setTotal(Math.round((invoice.getSubtotal() + taxAmount) * 100.0) / 100.0);
        }
        return mapToDto(invoiceRepository.save(invoice));
    }

    @Transactional
    public void deleteInvoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));
        invoiceRepository.delete(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceDto> getAllInvoices(Long projectId, Long clientId, InvoiceStatus status) {
        List<Invoice> invoices;
        if (projectId != null) {
            invoices = invoiceRepository.findByProjectId(projectId);
        } else if (clientId != null) {
            invoices = invoiceRepository.findByClientId(clientId);
        } else if (status != null) {
            invoices = invoiceRepository.findByStatus(status);
        } else {
            invoices = invoiceRepository.findAll();
        }
        return invoices.stream()
                .sorted(Comparator.comparing(Invoice::getCreatedAt).reversed())
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InvoiceDto getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));
        return mapToDto(invoice);
    }

    public byte[] generateInvoicePdf(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));
        return InvoicePdfRenderer.render(invoice);
    }

    @Transactional(readOnly = true)
    public BillingPreviewDto previewBilling(Long projectId, LocalDate fromDate, LocalDate toDate) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        List<InvoiceLineItem> lineItems = computeLineItems(project, fromDate, toDate);
        double subtotal = lineItems.stream().mapToDouble(InvoiceLineItem::getAmount).sum();
        double taxRate = 18.0;
        double taxAmount = Math.round(subtotal * taxRate / 100.0 * 100.0) / 100.0;

        Client client = project.getClient();

        return BillingPreviewDto.builder()
                .projectId(project.getId())
                .projectCode(project.getProjectCode())
                .projectName(project.getName())
                .clientName(client != null ? client.getName() : "N/A")
                .totalBillableHours(lineItems.stream().mapToDouble(InvoiceLineItem::getHours).sum())
                .totalAmount(subtotal)
                .taxRate(taxRate)
                .taxAmount(taxAmount)
                .grandTotal(Math.round((subtotal + taxAmount) * 100.0) / 100.0)
                .lineItems(lineItems.stream().map(this::mapLineItemToDto).collect(Collectors.toList()))
                .build();
    }

    @Transactional(readOnly = true)
    public BillingSummaryDto getBillingSummary() {
        List<Invoice> allInvoices = invoiceRepository.findAll();

        double totalRevenueBilled = allInvoices.stream().mapToDouble(Invoice::getTotal).sum();
        double totalRevenuePaid = allInvoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.PAID)
                .mapToDouble(Invoice::getTotal).sum();

        List<InvoiceLineItem> allLineItems = allInvoices.stream()
                .flatMap(i -> i.getLineItems().stream())
                .collect(Collectors.toList());

        Map<Long, Double> billedHoursPerProject = allLineItems.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getInvoice().getProject().getId(),
                        Collectors.summingDouble(InvoiceLineItem::getHours)));

        List<Project> allProjects = projectRepository.findAll();
        List<ProjectBillingSummaryDto> projectSummaries = new ArrayList<>();

        for (Project project : allProjects) {
            List<TimeEntry> entries = timeEntryRepository.findByProjectId(project.getId());
            double billableHours = entries.stream()
                    .filter(e -> e.getStatus() == TimeEntryStatus.APPROVED)
                    .filter(e -> e.getWorkItem() == null || "Billable".equalsIgnoreCase(e.getWorkItem().getBillingType()))
                    .filter(e -> e.getWorkDate() != null)
                    .mapToDouble(TimeEntry::getTotalHours)
                    .sum();

            double billedHrs = billedHoursPerProject.getOrDefault(project.getId(), 0.0);
            double unbilledHrs = Math.max(billableHours - billedHrs, 0);

            double avgRate = computeAvgHourlyRate(project.getId());
            double potentialRevenue = unbilledHrs * avgRate;

            projectSummaries.add(ProjectBillingSummaryDto.builder()
                    .projectId(project.getId())
                    .projectCode(project.getProjectCode())
                    .projectName(project.getName())
                    .clientName(project.getClient() != null ? project.getClient().getName() : "N/A")
                    .totalBillableApprovedHours((long) billableHours)
                    .billedHours((long) billedHrs)
                    .unbilledHours((long) unbilledHrs)
                    .avgHourlyRate(Math.round(avgRate * 100.0) / 100.0)
                    .potentialRevenue(Math.round(potentialRevenue * 100.0) / 100.0)
                    .build());
        }

        return BillingSummaryDto.builder()
                .totalRevenueBilled(Math.round(totalRevenueBilled * 100.0) / 100.0)
                .totalRevenuePaid(Math.round(totalRevenuePaid * 100.0) / 100.0)
                .totalOutstanding(Math.round((totalRevenueBilled - totalRevenuePaid) * 100.0) / 100.0)
                .invoiceCount(allInvoices.size())
                .paidInvoiceCount(allInvoices.stream().filter(i -> i.getStatus() == InvoiceStatus.PAID).count())
                .pendingInvoiceCount(allInvoices.stream().filter(i -> i.getStatus() == InvoiceStatus.DRAFT || i.getStatus() == InvoiceStatus.SENT).count())
                .projects(projectSummaries)
                .build();
    }

    private List<InvoiceLineItem> computeLineItems(Project project, LocalDate fromDate, LocalDate toDate) {
        List<TimeEntry> entries = timeEntryRepository.findByProjectId(project.getId());

        List<TimeEntry> billableEntries = entries.stream()
                .filter(e -> e.getStatus() == TimeEntryStatus.APPROVED)
                .filter(e -> e.getWorkDate() != null && !e.getWorkDate().isBefore(fromDate) && !e.getWorkDate().isAfter(toDate))
                .filter(e -> e.getWorkItem() == null || "Billable".equalsIgnoreCase(e.getWorkItem().getBillingType()))
                .collect(Collectors.toList());

        Map<String, List<TimeEntry>> grouped = billableEntries.stream()
                .collect(Collectors.groupingBy(e -> {
                    String workItemKey = e.getWorkItem() != null ? String.valueOf(e.getWorkItem().getId()) : "no-task";
                    String userKey = String.valueOf(e.getUser().getId());
                    return workItemKey + "-" + userKey;
                }));

        List<InvoiceLineItem> lineItems = new ArrayList<>();
        Set<Long> workItemIds = billableEntries.stream()
                .filter(e -> e.getWorkItem() != null)
                .map(e -> e.getWorkItem().getId())
                .collect(Collectors.toSet());

        for (Map.Entry<String, List<TimeEntry>> entry : grouped.entrySet()) {
            List<TimeEntry> group = entry.getValue();
            TimeEntry representative = group.get(0);

            WorkItem wi = representative.getWorkItem();
            User user = representative.getUser();
            double totalHours = group.stream().mapToDouble(TimeEntry::getTotalHours).sum();
            // TODO: Use actual contract/project billing rate instead of internal employee cost
            Double rate = user.getHourlyCost() != null ? user.getHourlyCost() : 0.0;
            double amount = Math.round(totalHours * rate * 100.0) / 100.0;

            lineItems.add(InvoiceLineItem.builder()
                    .workItemId(wi != null ? wi.getId() : null)
                    .workItemNumber(wi != null ? wi.getTicketNumber() : null)
                    .workItemTitle(wi != null ? wi.getTitle() : representative.getDescription())
                    .consultantId(user.getId())
                    .consultantName(user.getFullName())
                    .billingType(wi != null ? wi.getBillingType() : "Billable")
                    .description(wi != null ? wi.getTitle() : representative.getDescription())
                    .hours(Math.round(totalHours * 100.0) / 100.0)
                    .rate(rate)
                    .amount(amount)
                    .build());
        }

        return lineItems;
    }

    private double computeAvgHourlyRate(Long projectId) {
        List<TimeEntry> entries = timeEntryRepository.findByProjectId(projectId);
        return entries.stream()
                .filter(e -> e.getStatus() == TimeEntryStatus.APPROVED)
                .filter(e -> e.getWorkItem() == null || "Billable".equalsIgnoreCase(e.getWorkItem().getBillingType()))
                .mapToDouble(e -> e.getUser().getHourlyCost() != null ? e.getUser().getHourlyCost() : 0.0)
                .average()
                .orElse(0.0);
    }

    private String generateInvoiceNumber() {
        String yearPrefix = "INV-" + LocalDate.now().getYear() + "-";
        int maxOrdinal = invoiceRepository.findInvoiceNumbersByPrefix(yearPrefix).stream()
                .mapToInt(number -> {
                    int lastDash = number.lastIndexOf('-');
                    try {
                        return lastDash >= 0 ? Integer.parseInt(number.substring(lastDash + 1)) : 0;
                    } catch (NumberFormatException ex) {
                        return 0;
                    }
                })
                .max()
                .orElse(0);
        return yearPrefix + String.format("%03d", maxOrdinal + 1);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return userRepository.findByEmail(auth.getName()).orElse(null);
        }
        return null;
    }

    private InvoiceLineItemDto mapLineItemToDto(InvoiceLineItem item) {
        return InvoiceLineItemDto.builder()
                .id(item.getId())
                .workItemId(item.getWorkItemId())
                .workItemNumber(item.getWorkItemNumber())
                .workItemTitle(item.getWorkItemTitle())
                .consultantId(item.getConsultantId())
                .consultantName(item.getConsultantName())
                .billingType(item.getBillingType())
                .description(item.getDescription())
                .hours(item.getHours())
                .rate(item.getRate())
                .amount(item.getAmount())
                .build();
    }

    public InvoiceDto mapToDto(Invoice invoice) {
        List<InvoiceLineItemDto> lineItemsDto = invoice.getLineItems().stream()
                .map(this::mapLineItemToDto)
                .collect(Collectors.toList());

        Client client = invoice.getClient();
        Project project = invoice.getProject();

        return InvoiceDto.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .clientId(client != null ? client.getId() : null)
                .clientName(client != null ? client.getName() : "N/A")
                .clientContactPerson(client != null ? client.getContactPerson() : null)
                .clientEmail(client != null ? client.getEmail() : null)
                .clientAddress(client != null ? client.getAddress() : null)
                .projectId(project.getId())
                .projectCode(project.getProjectCode())
                .projectName(project.getName())
                .fromDate(invoice.getFromDate())
                .toDate(invoice.getToDate())
                .issuedDate(invoice.getIssuedDate())
                .dueDate(invoice.getDueDate())
                .status(invoice.getStatus())
                .subtotal(invoice.getSubtotal())
                .taxRate(invoice.getTaxRate())
                .taxAmount(invoice.getTaxAmount())
                .total(invoice.getTotal())
                .currency(invoice.getCurrency())
                .notes(invoice.getNotes())
                .createdByName(invoice.getCreatedByUser() != null ? invoice.getCreatedByUser().getFullName() : null)
                .lineItems(lineItemsDto)
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }
}
