import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule } from '@angular/material/dialog';
import { ExportService } from '../../core/services/export.service';
import { ExportDocument, ExportFormat, ExportColumn } from '../../core/models/report.models';

import { ProjectService } from '../../core/services/project.service';
import { WorkItemService } from '../../core/services/work-item.service';
import { SupportTicketService } from '../../core/services/support-ticket.service';

const FORMATS_LABELS: Record<ExportFormat, string> = {
  pdf: 'PDF',
  excel: 'Excel (.xlsx)',
  csv: 'CSV',
  word: 'Word (.docx)',
  txt: 'Plain Text',
  ppt: 'PowerPoint'
};

interface ReportCategoryOption {
  id: string;
  name: string;
  icon: string;
  availableColumns: { key: string; label: string; selected: boolean }[];
}

@Component({
  selector: 'app-reports-hub',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatTooltipModule, MatDialogModule],
  templateUrl: './reports-hub.component.html',
  styleUrls: ['./reports-hub.component.scss']
})
export class ReportsHubComponent implements OnInit {
  public activeTab = signal<'PORTFOLIO' | 'CUSTOM_BUILDER'>('PORTFOLIO');
  public toast = signal<string>('');
  public isLoadingPreview = signal<boolean>(false);

  // Custom Builder Controls
  public selectedCategory = signal<string>('workitems');
  public filterStatus = signal<string>('ALL');
  public filterProject = signal<string>('ALL');
  public startDateInput = '';
  public endDateInput = '';

  // Builder Datasets
  public categoryColumns = signal<{ key: string; label: string; selected: boolean }[]>([]);
  public livePreviewRows = signal<Record<string, any>[]>([]);
  public liveSummaryKpis = signal<{ label: string; value: any }[]>([]);
  public totalRecordCount = signal<number>(0);

  public formats = Object.keys(FORMATS_LABELS) as ExportFormat[];
  public formatLabel = FORMATS_LABELS;

  public categories: ReportCategoryOption[] = [
    {
      id: 'workitems',
      name: 'Work Items & Tasks',
      icon: 'task_alt',
      availableColumns: [
        { key: 'Ticket #', label: 'Ticket Code', selected: true },
        { key: 'Title', label: 'Task Title', selected: true },
        { key: 'Project', label: 'Project', selected: true },
        { key: 'Status', label: 'Status', selected: true },
        { key: 'Priority', label: 'Priority', selected: true },
        { key: 'Assignee', label: 'Assignee', selected: true },
        { key: 'Est Hours', label: 'Estimated Hours', selected: true },
        { key: 'Act Hours', label: 'Logged Hours', selected: true },
        { key: 'Billing Type', label: 'Billing Type', selected: false },
        { key: 'Jira ID', label: 'Jira Reference', selected: false }
      ]
    },
    {
      id: 'projects',
      name: 'Project Portfolio',
      icon: 'account_tree',
      availableColumns: [
        { key: 'Project Code', label: 'Project Code', selected: true },
        { key: 'Project Name', label: 'Project Name', selected: true },
        { key: 'Client', label: 'Client Name', selected: true },
        { key: 'Status', label: 'Status', selected: true },
        { key: 'Health', label: 'Delivery Health', selected: true },
        { key: 'Completion', label: 'Completion %', selected: true },
        { key: 'Estimated Hours', label: 'Est Budget Hours', selected: true },
        { key: 'Actual Hours', label: 'Logged Hours', selected: true }
      ]
    },
    {
      id: 'requirements',
      name: 'Requirements & Scope',
      icon: 'assignment',
      availableColumns: [
        { key: 'Req Number', label: 'Req Number', selected: true },
        { key: 'Title', label: 'Requirement Title', selected: true },
        { key: 'Project', label: 'Project Code', selected: true },
        { key: 'Status', label: 'Approval Status', selected: true },
        { key: 'Priority', label: 'Priority', selected: true },
        { key: 'Requester', label: 'Stakeholder', selected: true },
        { key: 'Est Effort', label: 'Est Effort', selected: true },
        { key: 'Actual Effort', label: 'Actual Effort', selected: true }
      ]
    },
    {
      id: 'timelogs',
      name: 'Time & Effort Audit',
      icon: 'schedule',
      availableColumns: [
        { key: 'ID', label: 'Log ID', selected: true },
        { key: 'User', label: 'Employee Name', selected: true },
        { key: 'Project', label: 'Project Code', selected: true },
        { key: 'Work Date', label: 'Work Date', selected: true },
        { key: 'Hours', label: 'Logged Hours', selected: true },
        { key: 'Description', label: 'Task Activity', selected: true },
        { key: 'Status', label: 'Approval Status', selected: true }
      ]
    },
    {
      id: 'risks',
      name: 'Risks & Issues',
      icon: 'warning',
      availableColumns: [
        { key: 'Code', label: 'Code', selected: true },
        { key: 'Type', label: 'Type (Risk/Issue)', selected: true },
        { key: 'Project', label: 'Project', selected: true },
        { key: 'Description', label: 'Description', selected: true },
        { key: 'Severity / Score', label: 'Severity Score', selected: true },
        { key: 'Status', label: 'Status', selected: true },
        { key: 'Owner', label: 'Risk Owner', selected: true }
      ]
    },
    {
      id: 'support-tickets',
      name: 'Support Tickets Desk',
      icon: 'confirmation_number',
      availableColumns: [
        { key: 'Ticket Code', label: 'Ticket Code', selected: true },
        { key: 'Subject', label: 'Subject', selected: true },
        { key: 'Category', label: 'Category', selected: true },
        { key: 'Priority', label: 'Priority', selected: true },
        { key: 'Target Role', label: 'Target Admin Role', selected: true },
        { key: 'Status', label: 'Resolution Status', selected: true },
        { key: 'Raised By', label: 'Raised By User', selected: true },
        { key: 'Assigned Admin', label: 'Assigned Admin', selected: true }
      ]
    }
  ];

  public cards = [
    { id: 'projects', icon: 'account_tree', title: 'Project Portfolio Status', description: 'Real-time health, budget, hours and milestone completion across all projects.' },
    { id: 'workitems', icon: 'task_alt', title: 'Work Items & Delivery Board', description: 'Live tasks, bugs and stories grouped by status, priority and assignees.' },
    { id: 'requirements', icon: 'assignment', title: 'Requirements & Scope Register', description: 'Change requests, scope-creep flags and effort variance per requirement.' },
    { id: 'timelogs', icon: 'schedule', title: 'Time & Effort Audit Log', description: 'Estimated vs actual hours and logged effort breakdown across teams.' },
    { id: 'capacity', icon: 'trending_up', title: 'Resource Capacity & Forecast', description: 'Team utilization, overload detection and project delivery projections.' },
    { id: 'risks', icon: 'warning', title: 'Risk & Issue Register', description: 'Probability-impact matrix, mitigations and open issues per project.' },
    { id: 'support-tickets', icon: 'confirmation_number', title: 'Support Tickets Telemetry', description: 'Super Admin & Admin support queries resolution times and open tickets.' }
  ];

  constructor(
    private exportService: ExportService,
    private http: HttpClient,
    private projectService: ProjectService,
    private workItemService: WorkItemService,
    private supportTicketService: SupportTicketService
  ) {}

  ngOnInit(): void {
    this.onCategoryChange('workitems');
  }

  public onCategoryChange(catId: string): void {
    this.selectedCategory.set(catId);
    const found = this.categories.find(c => c.id === catId);
    if (found) {
      this.categoryColumns.set(JSON.parse(JSON.stringify(found.availableColumns)));
    }
    this.fetchCustomReportData();
  }

  public toggleColumn(colKey: string): void {
    this.categoryColumns.update(cols =>
      cols.map(c => c.key === colKey ? { ...c, selected: !c.selected } : c)
    );
  }

  public selectAllColumns(select: boolean): void {
    this.categoryColumns.update(cols =>
      cols.map(c => ({ ...c, selected: select }))
    );
  }

  public selectedColumnsList = computed(() => {
    return this.categoryColumns().filter(c => c.selected);
  });

  public fetchCustomReportData(): void {
    this.isLoadingPreview.set(true);
    const cat = this.selectedCategory();
    const status = this.filterStatus();

    // Try fetching from Spring Boot REST API (/api/v1/reports/data/{category}) with fallback
    this.http.get<any>(`/api/v1/reports/data/${cat}?status=${status}`).subscribe({
      next: (res) => {
        this.isLoadingPreview.set(false);
        if (res?.data) {
          this.livePreviewRows.set(res.data.rows || []);
          this.liveSummaryKpis.set(res.data.summaryKpis || []);
          this.totalRecordCount.set(res.data.totalRecords || (res.data.rows ? res.data.rows.length : 0));
        } else {
          this.loadFallbackData(cat);
        }
      },
      error: () => {
        // Direct local service fallback
        this.loadFallbackData(cat);
      }
    });
  }

  private loadFallbackData(cat: string): void {
    this.isLoadingPreview.set(false);
    if (cat === 'workitems') {
      this.workItemService.getWorkItems().subscribe((res: any) => {
        const items: any[] = res?.data || [];
        const rows = items.map((w: any) => ({
          'Ticket #': w.ticketNumber,
          'Title': w.title,
          'Project': w.projectCode || '',
          'Status': w.status,
          'Priority': w.priority,
          'Assignee': w.assigneeName || 'Unassigned',
          'Est Hours': w.estimatedHours || 0,
          'Act Hours': w.actualHours || 0,
          'Billing Type': w.billingType || 'Billable',
          'Jira ID': w.jiraTaskId || '-'
        }));
        this.livePreviewRows.set(rows);
        this.liveSummaryKpis.set([
          { label: 'Total Tasks & Bugs', value: rows.length },
          { label: 'Completed Deliveries', value: items.filter((i: any) => i.status === 'COMPLETED' || i.status === 'UAT_EXIT').length }
        ]);
        this.totalRecordCount.set(rows.length);
      });
    } else if (cat === 'projects') {
      this.projectService.getAllProjects().subscribe((res: any) => {
        const projs: any[] = res?.data || [];
        const rows = projs.map((p: any) => ({
          'Project Code': p.projectCode,
          'Project Name': p.name,
          'Client': p.clientName || '',
          'Status': p.status,
          'Health': p.health || 'GREEN',
          'Completion': `${p.completionPercentage || 0}%`,
          'Estimated Hours': p.estimatedHours || 0,
          'Actual Hours': p.actualHours || 0
        }));
        this.livePreviewRows.set(rows);
        this.liveSummaryKpis.set([
          { label: 'Total Projects', value: rows.length },
          { label: 'Active Delivery Projects', value: projs.filter((p: any) => p.status === 'IN_PROGRESS' || p.status === 'APPROVED').length }
        ]);
        this.totalRecordCount.set(rows.length);
      });
    } else if (cat === 'support-tickets') {
      const tickets = this.supportTicketService.tickets();
      const rows = tickets.map(t => ({
        'Ticket Code': t.ticketCode,
        'Subject': t.subject,
        'Category': t.category,
        'Priority': t.priority,
        'Target Role': t.targetRole === 'ROLE_SUPER_ADMIN' ? '⚡ Super Admin' : '🛡️ Admin',
        'Status': t.status,
        'Raised By': t.createdByName,
        'Assigned Admin': t.assignedToName || ''
      }));
      this.livePreviewRows.set(rows);
      this.liveSummaryKpis.set([
        { label: 'Total Support Tickets', value: rows.length },
        { label: 'Active Open Queries', value: tickets.filter(t => t.status === 'OPEN' || t.status === 'IN_REVIEW').length }
      ]);
      this.totalRecordCount.set(rows.length);
    }
  }

  public async exportCustomReport(format: ExportFormat): Promise<void> {
    const selectedCols = this.selectedColumnsList();
    if (selectedCols.length === 0) {
      this.toast.set('Please select at least one field column to export.');
      return;
    }

    const exportCols: ExportColumn[] = selectedCols.map(c => ({
      key: c.key,
      label: c.label
    }));

    const rows = this.livePreviewRows();

    const doc: ExportDocument = {
      title: `${this.categories.find(c => c.id === this.selectedCategory())?.name || 'Custom'} Delivery Report`,
      subtitle: `Real-time database generated report`,
      summary: this.liveSummaryKpis(),
      sections: [
        {
          title: 'Custom Filtered Data Records',
          columns: exportCols,
          rows: rows
        }
      ]
    };

    const fileName = `TicketBoard_${this.selectedCategory()}_${new Date().toISOString().substring(0, 10)}`;

    try {
      this.toast.set(`Generating ${format.toUpperCase()} export...`);
      await this.exportService.export(doc, format, fileName);
      this.toast.set(`Report exported successfully as ${format.toUpperCase()}!`);
    } catch (err: any) {
      this.toast.set('Export failed: ' + (err?.message || 'Unexpected error'));
    }
  }

  public async previewCard(cardId: string, format: ExportFormat): Promise<void> {
    this.onCategoryChange(cardId);
    this.activeTab.set('CUSTOM_BUILDER');
  }
}