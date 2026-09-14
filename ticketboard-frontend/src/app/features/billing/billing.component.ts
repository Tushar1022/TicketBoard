import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSelectModule } from '@angular/material/select';
import { MatDialog } from '@angular/material/dialog';
import { DocumentPreviewDialogComponent } from '../../shared/components/document-preview/document-preview-dialog.component';
import { BillingService } from '../../core/services/billing.service';
import { ProjectService } from '../../core/services/project.service';
import { ExportService } from '../../core/services/export.service';
import { ExportDocument } from '../../core/models/report.models';
import {
  BillingPreview,
  BillingSummary,
  InvoiceDto,
  InvoiceStatus,
  Project
} from '../../core/models/api.models';

const STATUS_STYLES: Record<string, string> = {
  DRAFT: 'badge-slate',
  SENT: 'badge-blue',
  VIEWED: 'badge-indigo',
  PARTIALLY_PAID: 'badge-amber',
  PAID: 'badge-green',
  OVERDUE: 'badge-red',
  CANCELLED: 'badge-orange'
};

@Component({
  selector: 'app-billing',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, MatIconModule, MatTooltipModule, MatSelectModule],
  templateUrl: './billing.component.html',
  styleUrls: ['./billing.component.scss']
})
export class BillingComponent implements OnInit {
  public invoices = signal<InvoiceDto[]>([]);
  public summary = signal<BillingSummary | null>(null);
  public projects = signal<Project[]>([]);
  public isLoading = signal<boolean>(true);

  public showCreatePanel = signal<boolean>(true);
  public creating = signal<boolean>(false);
  public preview = signal<BillingPreview | null>(null);
  public previewLoading = signal<boolean>(false);
  public previewError = signal<string>('');

  public form = {
    projectId: null as number | null,
    fromDate: this.dateOffset(0),
    toDate: this.dateOffset(0),
    taxRate: 18
  };

  public selectedInvoice = signal<InvoiceDto | null>(null);
  public statusUpdateFor = signal<number | null>(null);

  public statusStyles = STATUS_STYLES;
  public statusOptions: InvoiceStatus[] = ['DRAFT', 'SENT', 'VIEWED', 'PARTIALLY_PAID', 'PAID', 'OVERDUE', 'CANCELLED'];

  constructor(
    private billingService: BillingService,
    private projectService: ProjectService,
    private exportService: ExportService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.projectService.getAllProjects().subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.projects.set(res.data);
        }
      }
    });
    this.loadData();
  }

  private dateOffset(days: number): string {
    const d = new Date(Date.now() + days * 86400000);
    const p = (n: number) => n.toString().padStart(2, '0');
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
  }

  public loadData(): void {
    this.isLoading.set(true);
    Promise.all([
      this.billingService.getSummary().toPromise(),
      this.billingService.getAllInvoices().toPromise()
    ]).then(([summaryRes, invoicesRes]: any[]) => {
      if (summaryRes?.success && summaryRes.data) this.summary.set(summaryRes.data);
      if (invoicesRes?.success && invoicesRes.data) this.invoices.set(invoicesRes.data);
      this.isLoading.set(false);
    }).catch(() => this.isLoading.set(false));
  }

  public generatePreview(): void {
    if (!this.form.projectId) {
      this.previewError.set('Please select a project.');
      return;
    }
    this.previewLoading.set(true);
    this.previewError.set('');
    this.preview.set(null);
    this.billingService
      .previewBilling(this.form.projectId, this.form.fromDate, this.form.toDate)
      .subscribe({
        next: (res: any) => {
          this.previewLoading.set(false);
          if (res.success && res.data) {
            this.preview.set(res.data);
          } else {
            this.previewError.set(res.message || 'No billable approved time found for this period.');
          }
        },
        error: (err: any) => {
          this.previewLoading.set(false);
          this.previewError.set(err?.error?.message || 'Could not generate preview.');
        }
      });
  }

  public createInvoice(): void {
    const p = this.preview();
    if (!p) return;
    this.creating.set(true);
    this.billingService.createInvoice({
      projectId: p.projectId,
      fromDate: this.form.fromDate,
      toDate: this.form.toDate,
      taxRate: this.form.taxRate,
      dueDate: this.dateOffset(30),
      notes: `Auto-generated invoice for ${p.projectName}`
    }).subscribe({
      next: (res: any) => {
        this.creating.set(false);
        if (res.success && res.data) {
          this.preview.set(null);
          this.loadData();
          this.selectInvoice(res.data);
        }
      },
      error: (err: any) => {
        this.creating.set(false);
        this.previewError.set(err?.error?.message || 'Invoice creation failed.');
      }
    });
  }

  public selectInvoice(inv: InvoiceDto): void {
    this.selectedInvoice.set(inv);
  }

  public changeStatus(inv: InvoiceDto, status: InvoiceStatus): void {
    this.billingService.updateStatus(inv.id, status).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.loadData();
          this.statusUpdateFor.set(null);
        }
      }
    });
  }

  public confirmDelete(inv: InvoiceDto): void {
    if (!confirm(`Delete invoice ${inv.invoiceNumber}? This cannot be undone.`)) return;
    this.billingService.deleteInvoice(inv.id).subscribe({
      next: () => {
        if (this.selectedInvoice()?.id === inv.id) this.selectedInvoice.set(null);
        this.loadData();
      }
    });
  }

  public downloadPdf(inv: InvoiceDto): void {
    this.billingService.downloadInvoicePdf(inv.id, inv.invoiceNumber);
  }

  public previewPdf(inv: InvoiceDto): void {
    this.billingService.getInvoicePdfBlob(inv.id).subscribe({
      next: (blob: Blob) => {
        this.dialog.open(DocumentPreviewDialogComponent, {
          data: {
            title: `Invoice ${inv.invoiceNumber}`,
            subtitle: `${inv.projectName} | ${inv.clientName} | Status: ${inv.status}`,
            pdfBlob: blob,
            downloadLabel: 'Download PDF',
            onDownload: () => this.billingService.downloadInvoicePdf(inv.id, inv.invoiceNumber)
          },
          width: '1080px',
          maxWidth: '96vw',
          panelClass: 'document-preview-panel'
        });
      },
      error: () => alert('Could not load the invoice PDF. Please try again.')
    });
  }

  public exportInvoice(inv: InvoiceDto, format: 'pdf' | 'word' | 'excel' | 'csv'): void {
    const doc = this.buildInvoiceDocument(inv);
    this.exportService.export(doc, format, `TicketBoard_${inv.invoiceNumber}`).then(() => {});
  }

  private buildInvoiceDocument(inv: InvoiceDto): ExportDocument {
    return {
      title: `Invoice ${inv.invoiceNumber}`,
      subtitle: `${inv.projectName} | ${inv.clientName}`,
      meta: [
        { label: 'Project', value: `${inv.projectCode} — ${inv.projectName}` },
        { label: 'Client', value: inv.clientName || 'N/A' },
        { label: 'Billing Period', value: `${inv.fromDate} to ${inv.toDate}` },
        { label: 'Issued', value: inv.issuedDate || '' },
        { label: 'Due', value: inv.dueDate || '' },
        { label: 'Status', value: inv.status }
      ],
      summary: [
        { label: 'Subtotal', value: this.currency(inv.subtotal) },
        { label: `Tax (${inv.taxRate}%)`, value: this.currency(inv.taxAmount) },
        { label: 'Total', value: this.currency(inv.total) },
        { label: 'Line Items', value: String(inv.lineItems?.length || 0) }
      ],
      sections: [
        {
          title: 'Line Items',
          columns: [
            { key: 'workItemNumber', label: 'Ticket', width: 22 },
            { key: 'workItemTitle', label: 'Work Item', width: 34 },
            { key: 'consultantName', label: 'Consultant', width: 20 },
            { key: 'billingType', label: 'Billing Type', width: 14 },
            { key: 'hours', label: 'Hours', format: 'number', align: 'right' },
            { key: 'rate', label: 'Rate', format: 'currency', align: 'right' },
            { key: 'amount', label: 'Amount', format: 'currency', align: 'right' }
          ],
          rows: inv.lineItems || []
        }
      ],
      notes: inv.notes || ''
    };
  }

  public currency(v: any): string {
    return typeof v === 'number' ? `$${v.toLocaleString('en-US', { minimumFractionDigits: 2 })}` : String(v);
  }

  public fmtDate(v: any): string {
    if (!v) return '';
    return new Date(v).toLocaleDateString('en-IN');
  }

  public sumInvoice(): number {
    return this.invoices().reduce((acc, i) => acc + (i.total || 0), 0);
  }
}