import { Component, OnInit, signal, computed, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { ExportService } from '../../core/services/export.service';
import { ExportDocument, ExportFormat, ExportColumn, ExportOptions, ExportOrientation, ExportLayout, STATUS_OPTIONS, ReportPreset } from '../../core/models/report.models';
import { DocumentPreviewDialogComponent } from '../../shared/components/document-preview/document-preview-dialog.component';

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
  imports: [CommonModule, FormsModule, MatIconModule, MatTooltipModule, MatDialogModule, MatMenuModule, MatDividerModule],
  templateUrl: './reports-hub.component.html',
  styleUrls: ['./reports-hub.component.scss']
})
export class ReportsHubComponent implements OnInit {
  public toast = signal<string>('');
  public isLoadingPreview = signal<boolean>(false);

  // Custom Builder Controls
  public selectedCategory = signal<string>('workitems');
  public filterStatus = signal<string>('ALL');
  public filterProject = signal<string>('ALL');
  public searchQuery = signal<string>('');
  public sortColumn = signal<string>('');
  public sortDirection = signal<'asc' | 'desc'>('asc');
  public selectedPreset = signal<string>('');
  public startDateInput = '';
  public endDateInput = '';
  public exportOrientation = signal<ExportOrientation>('landscape');
  public exportLayout = signal<ExportLayout>('standard');
  public statusOptions = signal<string[]>([]);
  public projectOptions = signal<{ code: string; name: string }[]>([]);

  // Builder Datasets
  public categoryColumns = signal<{ key: string; label: string; selected: boolean; group?: string }[]>([]);
  public rawPreviewRows = signal<Record<string, any>[]>([]);
  public liveSummaryKpis = signal<{ label: string; value: any }[]>([]);
  public totalRecordCount = signal<number>(0);

  // Industry-Graded Presets
  public presets: ReportPreset[] = [
    {
      id: 'exec-summary',
      name: 'Executive Overview',
      description: 'High-level snapshot with core identifiers, project, status, and logged effort.',
      icon: 'stars',
      categoryId: 'workitems',
      statusFilter: 'ALL',
      selectedColumnKeys: ['Ticket #', 'Title', 'Project', 'Status', 'Priority', 'Assignee', 'Act Hours']
    },
    {
      id: 'blockers-audit',
      name: 'Blockers & Critical Risks',
      description: 'Focus exclusively on blocked tasks, urgent priorities, and risk scores.',
      icon: 'warning',
      categoryId: 'workitems',
      statusFilter: 'BLOCKED',
      selectedColumnKeys: ['Ticket #', 'Title', 'Project', 'Status', 'Priority', 'Assignee']
    },
    {
      id: 'effort-audit',
      name: 'Effort & Time Audit',
      description: 'Detailed hours breakdown comparing estimated vs logged actual hours.',
      icon: 'hourglass_top',
      categoryId: 'timelogs',
      statusFilter: 'ALL',
      selectedColumnKeys: ['ID', 'User', 'Project', 'Work Date', 'Hours', 'Status']
    },
    {
      id: 'project-health',
      name: 'Portfolio Health Dashboard',
      description: 'Project delivery health, completion percentage, and client status.',
      icon: 'domain',
      categoryId: 'projects',
      statusFilter: 'ALL',
      selectedColumnKeys: ['Project Code', 'Project Name', 'Client', 'Status', 'Health', 'Completion %']
    }
  ];

  public livePreviewRows = computed(() => {
    let rows = [...this.rawPreviewRows()];
    const query = this.searchQuery().trim().toLowerCase();

    // 1. Text Search Filter across all fields
    if (query) {
      rows = rows.filter(row =>
        Object.values(row).some(val => String(val || '').toLowerCase().includes(query))
      );
    }

    // 2. Sorting
    const sortCol = this.sortColumn();
    const dir = this.sortDirection() === 'asc' ? 1 : -1;
    if (sortCol) {
      rows.sort((a, b) => {
        const valA = a[sortCol] ?? '';
        const valB = b[sortCol] ?? '';
        if (typeof valA === 'number' && typeof valB === 'number') {
          return (valA - valB) * dir;
        }
        return String(valA).localeCompare(String(valB)) * dir;
      });
    }

    return rows;
  });

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

  constructor(
    private exportService: ExportService,
    private http: HttpClient,
    private dialog: MatDialog,
    private projectService: ProjectService,
    private workItemService: WorkItemService,
    private supportTicketService: SupportTicketService
  ) {}

  ngOnInit(): void {
    this.loadProjectOptions();
    this.isLoadingPreview.set(true);
    this.onCategoryChange('workitems');
    // Optionally schedule a realtime status refresh when preview data streams in
  }

  /** Populate the project filter from the real backend projects table (no hardcoded list). */
  private loadProjectOptions(): void {
    this.projectOptions.set([]);
    this.projectService.getAllProjects().subscribe({
      next: (res: any) => {
        const projs: any[] = res?.data || [];
        this.projectOptions.set(
          projs
            .filter((p) => p && (p.projectCode || p.code))
            .map((p) => ({ code: p.projectCode || p.code, name: p.name }))
        );
      },
      error: () => {
        this.projectOptions.set([]); // filter simply shows "All Projects" if projects API is unavailable
      }
    });
  }

  /** Realtime statuses: merge the DB enum baseline with statuses actually present in the live rows. */
  private loadRealtimeStatusOptions(): void {
    effect(() => {
      const live = this.livePreviewRows() || [];
      const seen = new Set<string>(STATUS_OPTIONS);
      live.forEach((r) => {
        const s = r['Status'];
        if (s && s !== 'Status' && String(s).trim()) seen.add(String(s));
      });
      const merged = Array.from(seen).sort();
      this.statusOptions.set(merged);
    }, { allowSignalWrites: true });
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

  public applyPreset(presetId: string): void {
    const preset = this.presets.find(p => p.id === presetId);
    if (!preset) return;

    this.selectedPreset.set(presetId);
    this.selectedCategory.set(preset.categoryId);
    this.filterStatus.set(preset.statusFilter);

    // Update column selections
    const currentCols = this.categoryColumns();
    const updated = currentCols.map(c => ({
      ...c,
      selected: preset.selectedColumnKeys.includes(c.key)
    }));
    this.categoryColumns.set(updated);

    this.fetchCustomReportData();
    this.toast.set(`Applied report preset: ${preset.name}`);
  }

  public fetchCustomReportData(): void {
    this.isLoadingPreview.set(true);
    const cat = this.selectedCategory();
    const status = this.filterStatus();
    const project = this.filterProject();

    // Try fetching from Spring Boot REST API (/api/v1/reports/data/{category}) with fallback
    let url = `/api/v1/reports/data/${cat}?status=${status}`;
    const params: string[] = [];
    if (project && project !== 'ALL') params.push(`projectCode=${encodeURIComponent(project)}`);
    if (this.startDateInput) params.push(`startDate=${encodeURIComponent(this.startDateInput)}`);
    if (this.endDateInput) params.push(`endDate=${encodeURIComponent(this.endDateInput)}`);
    url += params.length ? '&' + params.join('&') : '';

    this.http.get<any>(url).subscribe({
      next: (res) => {
        this.isLoadingPreview.set(false);
        if (res?.data) {
          let rows = res.data.rows || [];
          rows = this.applyClientFilters(rows);
          this.rawPreviewRows.set(rows);
          this.liveSummaryKpis.set(res.data.summaryKpis || []);
          this.totalRecordCount.set(rows.length);
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

  private applyClientFilters(rows: Record<string, any>[]): Record<string, any>[] {
    let filtered = rows;
    const project = this.filterProject();
    if (project && project !== 'ALL') {
      filtered = filtered.filter(r => {
        const proj = r['Project'] ?? r['Project Code'];
        return String(proj).toUpperCase().includes(String(project).toUpperCase());
      });
    }
    const start = this.startDateInput ? new Date(this.startDateInput).getTime() : null;
    const end = this.endDateInput ? new Date(this.endDateInput).getTime() : null;
    if (start || end) {
      filtered = filtered.filter((r) => {
        const candidates = [r['Work Date'], r['Created'], r['Due Date'], r['Planned Date']];
        const val = candidates.find((c) => c && !isNaN(new Date(c).getTime()));
        if (val === undefined) return true;
        const t = new Date(val).getTime();
        if (start && t < start) return false;
        if (end && t > end) return false;
        return true;
      });
    }
    return filtered;
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
        const filtered = this.applyClientFilters(rows);
        this.rawPreviewRows.set(filtered);
        this.liveSummaryKpis.set([]);
        this.totalRecordCount.set(filtered.length);
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
        const filtered = this.applyClientFilters(rows);
        this.rawPreviewRows.set(filtered);
        this.liveSummaryKpis.set([]);
        this.totalRecordCount.set(filtered.length);
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
      const filtered = this.applyClientFilters(rows);
      this.rawPreviewRows.set(filtered);
      this.liveSummaryKpis.set([]);
      this.totalRecordCount.set(filtered.length);
    }
  }

  public async exportCustomReport(format: ExportFormat, options?: ExportOptions): Promise<void> {
    if (this.selectedColumnsList().length === 0) {
      this.toast.set('Please select at least one field column to export.');
      return;
    }

    const doc = this.buildCustomReportDoc();

    const fileName = `TicketBoard_${this.selectedCategory()}_${new Date().toISOString().substring(0, 10)}`;

    try {
      this.toast.set(`Generating ${format.toUpperCase()} export...`);
      await this.exportService.export(doc, format, fileName, options || {
        orientation: this.exportOrientation(),
        layout: this.exportLayout()
      });
      this.toast.set(`Report exported successfully as ${format.toUpperCase()}!`);
    } catch (err: any) {
      this.toast.set('Export failed: ' + (err?.message || 'Unexpected error'));
    }
  }

  private buildCustomReportDoc(): ExportDocument {
    const exportCols: ExportColumn[] = this.selectedColumnsList().map(c => ({
      key: c.key,
      label: c.label
    }));

    return {
      title: `${this.categories.find(c => c.id === this.selectedCategory())?.name || 'Custom'} Delivery Report`,
      subtitle: `Real-time database generated report`,
      meta: [
        { label: 'Category', value: this.categories.find(c => c.id === this.selectedCategory())?.name || this.selectedCategory() },
        { label: 'Status Filter', value: this.filterStatus() },
        { label: 'Project', value: this.filterProject() },
        { label: 'Record Count', value: String(this.totalRecordCount()) }
      ],
      summary: this.liveSummaryKpis(),
      sections: [
        {
          title: 'Custom Filtered Data Records',
          columns: exportCols,
          rows: this.livePreviewRows()
        }
      ]
    };
  }

  public previewCustom(format: ExportFormat = 'pdf'): void {
    if (this.selectedColumnsList().length === 0) {
      this.toast.set('Please select at least one field column to preview.');
      return;
    }

    const doc = this.buildCustomReportDoc();
    const opts = {
      orientation: this.exportOrientation(),
      layout: this.exportLayout()
    };

    const html = this.exportService.renderHtmlPreview(doc, format, opts);

    const formatLabel = format.toUpperCase();
    const orient = (format === 'pdf' || format === 'excel') ? `${opts.orientation.toUpperCase()}  •  ` : '';

    this.dialog.open(DocumentPreviewDialogComponent, {
      data: {
        title: doc.title,
        subtitle: `${formatLabel}  •  ${orient}${opts.layout.toUpperCase()} layout  •  WYSIWYG preview`,
        html,
        downloadLabel: `Download ${formatLabel}`,
        onDownload: () => {
          this.exportCustomReport(format, opts).then(() => {});
        }
      },
      width: '1080px',
      maxWidth: '96vw',
      panelClass: 'document-preview-panel'
    });
  }

  public clearFilters(): void {
    this.filterStatus.set('ALL');
    this.filterProject.set('ALL');
    this.startDateInput = '';
    this.endDateInput = '';
    this.fetchCustomReportData();
    this.toast.set('All report filters have been reset.');
  }
}