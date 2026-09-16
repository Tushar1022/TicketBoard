import { Component, OnInit, computed, signal, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ProjectService } from '../../../core/services/project.service';
import { RequirementService } from '../../../core/services/requirement.service';
import { DragDropModule, CdkDragDrop, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { boardStatuses } from '../../../core/config/status.config';
import { WorkItemService } from '../../../core/services/work-item.service';
import { ReleaseService } from '../../../core/services/release.service';
import { RiskService } from '../../../core/services/risk.service';
import { CommentService } from '../../../core/services/comment.service';
import { DocumentService } from '../../../core/services/document.service';
import { TimeTrackingService } from '../../../core/services/timetracking.service';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { MatMenuModule } from '@angular/material/menu';
import { ResizableColumnDirective } from '../../../core/directives/resizable-column.directive';
import { TaskDetailDialogComponent } from '../../../shared/components/task-detail-dialog/task-detail-dialog.component';
import {
  ActivityLog,
  Comment,
  Issue,
  Milestone,
  Project,
  ProjectStats,
  ProjectDocument,
  Release,
  Requirement,
  Risk,
  TimeEntry,
  User,
  WorkItem,
  WorkItemStatus
} from '../../../core/models/api.models';
import { LogTimeDialogComponent } from '../../../shared/components/log-time-dialog/log-time-dialog.component';
import { ExportService } from '../../../core/services/export.service';
import { ExportDocument, ExportColumn, ExportOptions } from '../../../core/models/report.models';
import { DocumentPreviewDialogComponent } from '../../../shared/components/document-preview/document-preview-dialog.component';

@Component({
  selector: 'app-project-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, MatIconModule, MatTooltipModule, MatDialogModule, MatMenuModule, DragDropModule, ResizableColumnDirective, TaskDetailDialogComponent],
  templateUrl: './project-detail.component.html',
  styleUrls: ['./project-detail.component.scss']
})
export class ProjectDetailComponent implements OnInit {
  private destroyRef = inject(DestroyRef);
  public Math = Math;
  public projectId: number = 1;
  public project = signal<Project | null>(null);
  public users = signal<User[]>([]);
  public requirements = signal<Requirement[]>([]);
  public tasks = signal<WorkItem[]>([]);
  public timeEntries = signal<TimeEntry[]>([]);
  public releases = signal<Release[]>([]);
  public risks = signal<Risk[]>([]);
  public issues = signal<Issue[]>([]);
  public comments = signal<Comment[]>([]);
  public auditLogs = signal<ActivityLog[]>([]);
  public projectDocuments = signal<ProjectDocument[]>([]);
  public selectedDocFile = signal<File | null>(null);
  public activeTab = signal<string>('dashboard');
  public isLoading = signal<boolean>(true);
  public loadError = signal<string>('');
  public projectStats = signal<ProjectStats | null>(null);

  // Reports workbench
  public selectedPlotOption = signal<'count' | 'percent'>('count');

  // Milestone create modal
  public showCreateMilestoneModal = signal<boolean>(false);
  public newMilestone: any = { name: '', description: '', ownerId: null, planDate: '', plannedDate: '', completionPercentage: 0 };

  // Issues state
  public showUpdateIssueModal = signal<boolean>(false);
  public editingIssueId = signal<number | null>(null);
  public issueEditForm: any = { title: '', description: '', severity: 'MEDIUM', status: 'OPEN', resolution: '', classification: '', stepsToReproduce: '', crValue: null, crManDays: null, ownerId: null };

  // Comments view toggle
  public commentsView = signal<'discussion' | 'activity'>('discussion');

  // Task Filtering Dropdown & View Mode State
  public selectedTaskFilter = signal<string>('ALL');
  public taskViewMode = signal<'table' | 'grid' | 'kanban'>('table');

  // Dynamic Inline Edit Active State
  public inlineEditing = signal<{ id: number; field: string; entity: 'task' | 'project' | 'milestone' | 'timeEntry' } | null>(null);

  // Filtered Tasks Computation
  public filteredTasks = computed(() => {
    const filter = this.selectedTaskFilter();
    const now = new Date().toISOString().substring(0, 10);
    return this.tasks().filter((t) => {
      if (filter === 'OPEN') return t.status !== 'COMPLETED' && t.status !== 'CLOSED';
      if (filter === 'MY_OPEN') return (t.assigneeId === this.authService.currentUser()?.id || !t.assigneeId) && t.status !== 'COMPLETED' && t.status !== 'CLOSED';
      if (filter === 'DELAYED') return !!t.dueDate && t.dueDate < now && t.status !== 'COMPLETED' && t.status !== 'CLOSED';
      if (filter === 'HIGH_PRIORITY') return t.priority === 'HIGH' || t.priority === 'CRITICAL';
      if (filter === 'BLOCKED') return t.status === 'BLOCKED' || t.status === 'ON_HOLD';
      if (filter === 'DEV_EXIT') return t.status === 'DEV_EXIT' || t.status === 'Dev in progress';
      if (filter === 'SIT_EXIT') return t.status === 'SIT_EXIT' || t.status === 'SIT in progress';
      if (filter === 'UAT_EXIT') return t.status === 'UAT_EXIT' || t.status === 'UAT in progress';
      if (filter === 'COMPLETED') return t.status === 'COMPLETED' || t.status === 'CLOSED';
      return true;
    });
  });

  public kanbanBoardData = computed(() => {
    const kbData: Record<string, WorkItem[]> = {};
    const tasks = this.filteredTasks();
    const cols = boardStatuses().map((c) => ({ status: c.value, label: c.label, badgeClass: c.badgeClass }));
    cols.forEach(col => {
      kbData[col.status] = tasks.filter(t => this.normalizeStatus(t.status) === this.normalizeStatus(col.status));
    });
    return kbData;
  });

  // Milestones KPIs
  public milestoneKPIs = computed(() => {
    const list = this.project()?.milestones || [];
    return {
      total: list.length,
      achieved: list.filter((m) => m.status === 'ACHIEVED' || m.completionPercentage === 100).length,
      inProgress: list.filter((m) => m.status === 'IN_PROGRESS' || (m.completionPercentage > 0 && m.completionPercentage < 100)).length,
      missed: list.filter((m) => m.status === 'MISSED').length
    };
  });

  // Time Logs KPIs
  public timeLogMetrics = computed(() => {
    const entries = this.timeEntries();
    const totalLogged = entries.reduce((acc, e) => acc + (e.totalHours || 0), 0);
    const billableCount = entries.filter((e) => e.billingType === 'Billable').length;
    const billableRatio = entries.length ? Math.round((billableCount / entries.length) * 100) : 100;
    return {
      totalLogged: Math.round(totalLogged * 10) / 10,
      plannedHours: Math.round(totalLogged * 1.1 * 10) / 10,
      varianceHours: Math.round(totalLogged * 0.1 * 10) / 10,
      billableRatio
    };
  });

  // Dashboard KPIs (derived from live data)
  public totalIssues = computed(() => this.issues().length);
  public openIssues = computed(() =>
    this.issues().filter((i) => i.status === 'OPEN' || i.status === 'IN_PROGRESS').length
  );
  public overdueTasks = computed(() => {
    const now = new Date().toISOString().substring(0, 10);
    return this.tasks().filter(
      (t) => t.dueDate && t.dueDate < now && t.status !== 'COMPLETED' && t.status !== 'CLOSED'
    ).length;
  });
  public openTasksDueToday = computed(() => {
    const today = new Date().toISOString().substring(0, 10);
    return this.tasks().filter(
      (t) => t.dueDate === today && t.status !== 'COMPLETED' && t.status !== 'CLOSED'
    ).length;
  });

  public openTasksCount = computed(() => this.projectStats()?.openTasks ?? this.tasks().filter(
    (t) => t.status !== 'COMPLETED' && t.status !== 'CLOSED'
  ).length);
  public openBugs = computed(() => this.projectStats()?.openBugs ?? this.tasks().filter(
    (t) => t.type === 'BUG' && t.status !== 'COMPLETED' && t.status !== 'CLOSED'
  ).length);
  public reqCount = computed(() => this.projectStats()?.totalRequirements ?? this.requirements().length);
  public milestoneCount = computed(() => this.projectStats()?.totalMilestones ?? this.project()?.milestones?.length ?? 0);
  public memberCount = computed(() => this.projectStats()?.totalMembers ?? this.project()?.members?.length ?? 0);
  public milestoneAchieved = computed(() => this.projectStats()?.achievedMilestones ?? this.milestoneKPIs().achieved);
  public completionPct = computed(() => this.projectStats()?.completionPercentage ?? this.project()?.completionPercentage ?? 0);
  public totalPlannedHours = computed(() => this.projectStats()?.totalEstimatedHours ?? this.project()?.estimatedHours ?? 0);
  public totalLoggedHours = computed(() => this.projectStats()?.totalActualHours ?? this.project()?.actualHours ?? 0);

  public taskCount = computed(() => this.tasks().length);
  public taskStatusSummary = computed(() => {
    const counts = new Map<string, number>();
    for (const t of this.tasks()) {
      const label = (t.status || 'UNKNOWN').replace(/_/g, ' ');
      counts.set(label, (counts.get(label) || 0) + 1);
    }
    return Array.from(counts.entries())
      .map(([label, count]) => ({ label, count }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 6);
  });
  public donutSegments = computed(() => {
    const items = this.taskStatusSummary();
    const colors = ['#22c55e', '#3b82f6', '#f59e0b', '#8b5cf6', '#64748b', '#ef4444'];
    const total = items.reduce((acc, it) => acc + it.count, 0) || 1;
    let offset = 0;
    return items.map((it, idx) => {
      const percent = (it.count / total) * 100;
      const seg = {
        label: it.label,
        count: it.count,
        percent,
        offset,
        color: colors[idx % colors.length]
      };
      offset -= percent;
      return seg;
    });
  });
  public issueStatusSummary = computed(() => {
    const order = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
    const counts = new Map<string, number>();
    for (const i of this.issues()) {
      const status = i.status || 'OPEN';
      counts.set(status, (counts.get(status) || 0) + 1);
    }
    const list = Array.from(counts.entries()).map(([label, count]) => ({ label, count }));
    list.sort((a, b) => {
      const ia = order.indexOf(a.label);
      const ib = order.indexOf(b.label);
      return (ia === -1 ? 99 : ia) - (ib === -1 ? 99 : ib);
    });
    return list;
  });
  public issueBarMax = computed(() => {
    const max = Math.max(...this.issueStatusSummary().map((s) => s.count), 0);
    return max || 1;
  });

  public issueBarClass(status: string): string {
    switch (status) {
      case 'OPEN':
        return 'blue';
      case 'IN_PROGRESS':
        return 'amber';
      case 'RESOLVED':
        return 'green';
      case 'CLOSED':
        return 'slate';
      default:
        return 'blue';
    }
  }

  public taskPrioritySummary = computed(() => {
    const order = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
    const counts = new Map<string, number>();
    for (const t of this.tasks()) {
      const prio = t.priority || 'MEDIUM';
      counts.set(prio, (counts.get(prio) || 0) + 1);
    }
    const list = Array.from(counts.entries()).map(([label, count]) => ({ label, count }));
    list.sort((a, b) => order.indexOf(a.label) - order.indexOf(b.label));
    return list;
  });

  public issueSeveritySummary = computed(() => {
    const order = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
    const counts = new Map<string, number>();
    for (const i of this.issues()) {
      const sev = i.severity || 'MEDIUM';
      counts.set(sev, (counts.get(sev) || 0) + 1);
    }
    const list = Array.from(counts.entries()).map(([label, count]) => ({ label, count }));
    list.sort((a, b) => order.indexOf(a.label) - order.indexOf(b.label));
    return list;
  });

  public reportSeries = computed(() => {
    const plot = this.selectedPlotOption();
    const mapSeries = (items: { label: string; count: number }[], total: number) =>
      items.map(s => ({
        label: s.label,
        value: plot === 'percent' ? Math.round((s.count / (total || 1)) * 100) : s.count,
        rawCount: s.count,
        isPercent: plot === 'percent'
      }));

    if (this.selectedReportCategory() === 'Issue Basic Reports') {
      const total = this.issues().length;
      const source = this.selectedReportName() === 'Severity' ? this.issueSeveritySummary() : this.issueStatusSummary();
      return mapSeries(source, total);
    }
    const total = this.taskStatusSummary().reduce((acc, it) => acc + it.count, 0);
    const source = this.selectedReportName() === 'Priority' ? this.taskPrioritySummary() : this.taskStatusSummary();
    return mapSeries(source, total);
  });

  public reportSeriesTotal = computed(() => {
    return this.reportSeries().reduce((acc, s) => acc + s.rawCount, 0);
  });

  public reportTotal = computed(() => {
    if (this.selectedReportCategory() === 'Issue Basic Reports') return this.issues().length;
    return this.tasks().length;
  });

  public reportMaxValue = computed(() => {
    const series = this.reportSeries();
    const max = series.reduce((acc, s) => Math.max(acc, s.value), 0);
    return max || 1;
  });

  public reportColors = ['#6366f1', '#0ea5e9', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#14b8a6', '#ec4899', '#64748b', '#f97316'];

  public setChartType(type: 'bar' | 'pie' | 'donut'): void {
    this.selectedChartType.set(type);
  }

  public pieOffset(s: any, index: number): number {
    const series = this.reportSeries();
    let offset = 0;
    for (let i = 0; i < index; i++) {
      offset += series[i].value;
    }
    return -offset;
  }

  public setPlotOption(opt: 'count' | 'percent'): void {
    this.selectedPlotOption.set(opt);
  }

  public buildReportDocument(): ExportDocument {
    const project = this.project();
    const series = this.reportSeries();
    const isIssue = this.selectedReportCategory() === 'Issue Basic Reports';
    const sourceName = this.selectedReportName();
    const plot = this.selectedPlotOption();

    const rows = series.map(s => ({
      bucket: s.label,
      value: s.rawCount,
      share: series.length ? Math.round((s.rawCount / (this.reportTotal() || 1)) * 100) : 0
    }));

    const cols: ExportColumn[] = [
      { key: 'bucket', label: sourceName },
      { key: 'value', label: 'Count', align: 'right', format: 'number' },
      { key: 'share', label: 'Distribution %', align: 'right' }
    ];

    return {
      title: `${project?.projectCode || 'Project'} • ${sourceName}`,
      subtitle: `${isIssue ? 'Issue' : 'Task'} distribution report — ${new Date().toISOString().substring(0, 10)}`,
      meta: [
        { label: 'Project', value: `${project?.projectCode || '—'} ${project?.name || ''}` },
        { label: 'Client', value: project?.clientName || 'N/A' },
        { label: 'Report Category', value: this.selectedReportCategory() },
        { label: 'Plot Option', value: plot },
        { label: 'Base Records', value: String(this.reportTotal()) }
      ],
      summary: rows.map(r => ({ label: r.bucket, value: `${r.value} (${r.share}%)` })),
      sections: [
        {
          title: `${sourceName} — Distribution Table`,
          description: 'Count of records per bucket with proportionate distribution shares.',
          columns: cols,
          rows
        }
      ],
      notes: 'Generated by TicketBoard Enterprise Reports for project-level distribution analysis.'
    };
  }

  public exportReport(format: 'pdf' | 'excel' | 'csv', options?: ExportOptions): void {
    const doc = this.buildReportDocument();
    const fileName = `TicketBoard_${this.project()?.projectCode || 'Project'}_${this.selectedReportName().replace(/\s+/g, '_')}_${new Date().toISOString().substring(0, 10)}`;
    this.exportService.export(doc, format, fileName, options ? { orientation: options.orientation, layout: options.layout } : { layout: 'standard' }).then(() => {});
  }

  public previewReport(): void {
    const doc = this.buildReportDocument();
    const html = this.exportService.renderHtmlPreview(doc, 'pdf', { orientation: 'landscape', layout: 'standard' });
    this.dialog.open(DocumentPreviewDialogComponent, {
      data: {
        title: `${this.selectedReportName()} — Print Preview`,
        subtitle: `${this.project()?.projectCode || 'Project'}  •  Landscape A4  •  Charts + Table`,
        html,
        downloadLabel: 'Download PDF',
        onDownload: () => {
          this.exportReport('pdf', { orientation: 'landscape', layout: 'standard' });
        }
      },
      width: '1080px',
      maxWidth: '96vw',
      panelClass: 'document-preview-panel'
    });
  }

  // Task Detail Modal State (rendered by the shared TaskDetailDialogComponent)
  public selectedTask = signal<WorkItem | null>(null);
  public showTaskDetailModal = signal<boolean>(false);

  // Issues Master-Detail State
  public selectedIssue = signal<Issue | null>(null);
  public issueDetailTab = signal<string>('description');

  // New Comment (project-level discussion)
  public newCommentText = signal<string>('');
  public editingCommentId = signal<number | null>(null);
  public commentEditText = signal<string>('');

  // Reports Workbench State
  public selectedReportCategory = signal<string>('Task Basic Reports');
  public selectedReportName = signal<string>('Task Status Report');
  public selectedChartType = signal<string>('bar');

  // Create Task Modal State
  public showCreateTaskModal = signal<boolean>(false);
  public newTask: any = {
    title: '',
    description: '',
    type: 'TASK',
    priority: 'MEDIUM',
    status: 'TODO',
    projectId: 1,
    estimatedHours: 40,
    durationDays: 10,
    billingType: 'Billable',
    startDate: new Date().toISOString().substring(0, 10),
    dueDate: new Date(Date.now() + 86400000 * 10).toISOString().substring(0, 10)
  };

  // Create Issue Modal State
  public showCreateIssueModal = signal<boolean>(false);
  public newIssue: any = {
    title: '',
    description: '',
    severity: 'MEDIUM',
    status: 'OPEN',
    projectId: 1,
    classification: '',
    dueDate: new Date(Date.now() + 86400000 * 7).toISOString().substring(0, 10)
  };

  constructor(
    private route: ActivatedRoute,
    private projectService: ProjectService,
    private requirementService: RequirementService,
    private workItemService: WorkItemService,
    private releaseService: ReleaseService,
    private riskService: RiskService,
    private commentService: CommentService,
    private documentService: DocumentService,
    private timeTrackingService: TimeTrackingService,
    private userService: UserService,
    private dialog: MatDialog,
    private exportService: ExportService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.userService.getUsers({ status: 'ACTIVE' }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success && res.data) this.users.set(res.data);
      }
    });
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params: any) => {
      const id = params.get('id');
      if (id) {
        this.projectId = Number(id);
        this.newTask.projectId = this.projectId;
        this.newIssue.projectId = this.projectId;
        this.loadAllProjectData();
      }
    });
  }

  public setTab(tab: string): void {
    this.activeTab.set(tab);
  }

    public loadAllProjectData(): void {
    this.isLoading.set(true);
    this.loadError.set('');

    forkJoin({
      project: this.projectService.getProjectById(this.projectId),
      reqs: this.requirementService.getAllRequirements({ projectId: this.projectId }),
      tasks: this.workItemService.getWorkItems({ projectId: this.projectId }),
      time: this.timeTrackingService.getTimeEntriesByProject(this.projectId),
      releases: this.releaseService.getReleases(this.projectId),
      risks: this.riskService.getRisks(this.projectId),
      issues: this.riskService.getIssuesByProject(this.projectId)
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.project.success && res.project.data) this.project.set(res.project.data);
        if (res.reqs.success && res.reqs.data) this.requirements.set(res.reqs.data);
        if (res.tasks.success && res.tasks.data) {
          this.tasks.set(res.tasks.data);
          if (res.tasks.data.length && !this.selectedTask()) this.selectedTask.set(res.tasks.data[0]);
        }
        if (res.time.success && res.time.data) this.timeEntries.set(res.time.data);
        if (res.releases.success && res.releases.data) this.releases.set(res.releases.data);
        if (res.risks.success && res.risks.data) this.risks.set(res.risks.data);
        if (res.issues.success && res.issues.data) this.issues.set(res.issues.data);

        this.loadComments();
        this.loadAuditLogs();
        this.loadProjectDocuments();
        this.loadProjectStats();
        this.isLoading.set(false);
      },
      error: () => {
        this.loadError.set('Failed to load project details.');
        this.isLoading.set(false);
      }
    });
  }

  public loadProjectStats(): void {
    this.projectService.getProjectStats(this.projectId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success && res.data) this.projectStats.set(res.data);
      }
    });
  }

  public loadComments(): void {
    this.commentService.getComments('PROJECT', this.projectId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.comments.set(res.data);
        }
      }
    });
  }

  public loadAuditLogs(): void {
    this.commentService.getAuditLogs('PROJECT', this.projectId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.auditLogs.set(res.data);
        }
      }
    });
  }

  public loadProjectDocuments(): void {
    this.documentService.getProjectDocuments(this.projectId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.projectDocuments.set(res.data);
        }
      }
    });
  }

  public triggerDocFileInput(inputEl: HTMLInputElement): void {
    inputEl.click();
  }

  public onDocFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length ? input.files[0] : null;
    this.selectedDocFile.set(file);
  }

  public uploadProjectDoc(): void {
    const file = this.selectedDocFile();
    if (!file) return;
    this.documentService.uploadProjectDocument(this.projectId, file).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.selectedDocFile.set(null);
          this.loadProjectDocuments();
        }
      }
    });
  }

  public downloadDoc(doc: ProjectDocument): void {
    if (doc.downloadUrl) {
      this.documentService.openDownload(doc.downloadUrl);
    } else if (doc.fileUrl) {
      this.documentService.openDownload(doc.fileUrl);
    }
  }

  public deleteProjectDoc(doc: ProjectDocument): void {
    if (!confirm(`Delete document "${doc.fileName}"?`)) return;
    this.documentService.deleteProjectDocument(doc.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.loadProjectDocuments()
    });
  }

  public postComment(): void {
    if (!this.newCommentText().trim()) return;
    this.commentService.addComment({
      entityType: 'PROJECT',
      entityId: this.projectId,
      content: this.newCommentText()
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.newCommentText.set('');
          this.loadComments();
        }
      }
    });
  }

  public startEditComment(comment: Comment): void {
    this.editingCommentId.set(comment.id);
    this.commentEditText.set(comment.content);
  }

  public cancelEditComment(): void {
    this.editingCommentId.set(null);
    this.commentEditText.set('');
  }

  public saveCommentEdit(comment: Comment): void {
    const text = this.commentEditText().trim();
    if (!text || text === comment.content) {
      this.cancelEditComment();
      return;
    }
    this.commentService.updateComment(comment.id, text).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.cancelEditComment();
          this.loadComments();
        }
      }
    });
  }

  public deleteComment(comment: Comment): void {
    if (!confirm(`Delete this comment by ${comment.authorName || 'Unknown'}?\n\n"${comment.content.slice(0, 80)}${comment.content.length > 80 ? '...' : ''}"`)) return;
    this.commentService.deleteComment(comment.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success) {
          if (this.editingCommentId() === comment.id) this.cancelEditComment();
          this.loadComments();
        }
      }
    });
  }

  public setCommentsView(view: 'discussion' | 'activity'): void {
    this.commentsView.set(view);
  }

  public activityTypeIcon(eventType: string): string {
    switch (eventType) {
      case 'CREATED': return 'add_circle';
      case 'STATUS_CHANGED': return 'swap_horiz';
      case 'ASSIGNED': return 'person_add';
      case 'BLOCKED': return 'block';
      case 'UNBLOCKED': return 'lock_open';
      case 'COMMENT_ADDED': return 'chat_bubble';
      case 'COMMENT_EDITED': return 'edit';
      case 'COMMENT_DELETED': return 'delete_outline';
      case 'DOCUMENT_ATTACHED': return 'attach_file';
      case 'HOURS_LOGGED': return 'timer';
      case 'RELEASED': return 'rocket_launch';
      default: return 'schedule';
    }
  }

  public formatChatTime(iso?: string): string {
    if (!iso) return '';
    const d = new Date(iso);
    if (isNaN(d.getTime())) return iso;
    const sameDate = (a: Date, b: Date) =>
      a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
    const now = new Date();
    const hh = String(d.getHours()).padStart(2, '0');
    const mm = String(d.getMinutes()).padStart(2, '0');
    const timePart = `${hh}:${mm}`;
    if (sameDate(d, now)) return `Today, ${timePart}`;
    const yesterday = new Date(now);
    yesterday.setDate(now.getDate() - 1);
    if (sameDate(d, yesterday)) return `Yesterday, ${timePart}`;
    const dd = String(d.getDate()).padStart(2, '0');
    const mon = d.toLocaleString('en-GB', { month: 'short' });
    return `${dd} ${mon} ${d.getFullYear()}, ${timePart}`;
  }

  private avatarPalette = ['#6366f1', '#0ea5e9', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#14b8a6', '#ec4899'];

  public avatarColor(name?: string): string {
    if (!name) return this.avatarPalette[0];
    let hash = 0;
    for (let i = 0; i < name.length; i++) hash = (hash * 31 + name.charCodeAt(i)) % 997;
    return this.avatarPalette[hash % this.avatarPalette.length];
  }

  public openTaskDetail(task: WorkItem): void {
    this.selectedTask.set(task);
    this.showTaskDetailModal.set(true);
  }

  public closeTaskDetail(): void {
    this.showTaskDetailModal.set(false);
    this.selectedTask.set(null);
  }

  public onTaskModalSaved(): void {
    this.loadAllProjectData();
  }

  public selectIssue(issue: Issue): void {
    this.selectedIssue.set(issue);
  }

  public selectReport(category: string, name: string): void {
    this.selectedReportCategory.set(category);
    this.selectedReportName.set(name);
  }

  public deepLinkFilter(filter: string): void {
    if (filter === 'overdue' || filter === 'open_tasks' || filter === 'total_tasks') {
      this.setTab('tasks');
      if (filter === 'overdue') this.selectedTaskFilter.set('DELAYED');
      if (filter === 'open_tasks') this.selectedTaskFilter.set('OPEN');
      if (filter === 'total_tasks') this.selectedTaskFilter.set('ALL');
    } else if (filter === 'total_issues' || filter === 'open_issues') {
      this.setTab('issues');
    } else if (filter === 'requirements') {
      this.setTab('tasks');
    } else if (filter === 'milestones') {
      this.setTab('milestones');
    }
  }

  public taskStatusClass(status: string): string {
    switch (status) {
      case 'COMPLETED':
      case 'CLOSED':
        return 'green-pill';
      case 'BLOCKED':
      case 'ON_HOLD':
        return 'red-pill';
      case 'IN_PROGRESS':
      case 'DEV_IN_PROGRESS':
      case 'SIT_IN_PROGRESS':
      case 'UAT_IN_PROGRESS':
        return 'blue-pill';
      case 'GO_LIVE':
      case 'UAT_EXIT':
      case 'SIT_EXIT':
        return 'gold-pill';
      default:
        return 'slate-pill';
    }
  }

  public isOverdue(task: WorkItem): boolean {
    const now = new Date().toISOString().substring(0, 10);
    return !!task.dueDate && task.dueDate < now && task.status !== 'COMPLETED' && task.status !== 'CLOSED';
  }

  public daysLate(task: WorkItem): number {
    if (!task.dueDate || !this.isOverdue(task)) return 0;
    const due = new Date(task.dueDate).getTime();
    const now = Date.now();
    return Math.max(1, Math.round((now - due) / 86400000));
  }

  // Kanban Columns
  public get kanbanColumns(): { status: WorkItemStatus; label: string; badgeClass: string }[] {
    return boardStatuses().map((c) => ({ status: c.value, label: c.label, badgeClass: c.badgeClass }));
  }

  public openKanbanDetail(item: WorkItem): void {
    this.openTaskDetail(item);
  }

  public getColumnItems(status: string): WorkItem[] {
    return this.kanbanBoardData()[status] || [];
  }

  public getConnectedDropLists(): string[] {
    return this.kanbanColumns.map((col) => 'cdk-drop-list-' + col.status);
  }

  public onKanbanDrop(event: CdkDragDrop<WorkItem[], any>, targetStatus: string): void {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      const movedItem = event.previousContainer.data[event.previousIndex];
      transferArrayItem(
        event.previousContainer.data,
        event.container.data,
        event.previousIndex,
        event.currentIndex
      );
      movedItem.status = targetStatus as WorkItemStatus;
      this.updateTaskStatus(movedItem.id, targetStatus as WorkItemStatus, true);
    }
  }

  public updateTaskStatus(taskId: number, status: WorkItemStatus, silent: boolean = false): void {
    this.workItemService.updateStatus(taskId, status).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        if (!silent) this.loadAllProjectData();
      }
    });
  }

  private normalizeStatus(status: string | undefined): string {
    return (status || '').toUpperCase().replace(/[\s_-]/g, '');
  }

  // --- Dynamic Inline Editing Handlers ---
  public startCellEdit(entity: 'task' | 'project' | 'milestone' | 'timeEntry', id: number, field: string, event?: MouseEvent): void {
    if (event) event.stopPropagation();
    this.inlineEditing.set({ entity, id, field });
  }

  public stopCellEdit(): void {
    this.inlineEditing.set(null);
  }

  public isEditingCell(entity: string, id: number, field: string): boolean {
    const cur = this.inlineEditing();
    return !!cur && cur.entity === entity && cur.id === id && cur.field === field;
  }

  public updateTaskField(task: WorkItem, field: string, value: any): void {
    (task as any)[field] = value;
    this.workItemService.updateWorkItem(task.id, { [field]: value }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      error: (err) => console.error('Update failed:', err)
    });
    this.stopCellEdit();
  }

  public updateProjectField(field: string, value: any): void {
    const p = this.project();
    if (!p) return;
    (p as any)[field] = value;
    if (field === 'projectManagerId') {
      const user = this.users().find(u => String(u.id) === String(value));
      if (user) p.projectManagerName = user.fullName;
      value = Number(value);
    }
    this.project.set({ ...p });
    this.projectService.updateProject(p.id, { [field]: value }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      error: (err) => console.error('Update failed:', err)
    });
    this.stopCellEdit();
  }

  public updateMilestoneField(m: Milestone, field: string, value: any): void {
    this.projectService.updateMilestone(m.id, { [field]: value }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        if (this.project()) {
          const milestones = (this.project()?.milestones || []).map((mm) => (mm.id === m.id ? { ...mm, [field]: value } : mm));
          this.project.set({ ...this.project()!, milestones });
        }
      }
    });
    this.stopCellEdit();
  }

  public openCreateMilestoneModal(): void {
    this.newMilestone = { name: '', description: '', ownerId: this.authService.currentUser()?.id ?? null, plannedDate: '', completionPercentage: 0 };
    this.showCreateMilestoneModal.set(true);
  }

  public closeCreateMilestoneModal(): void {
    this.showCreateMilestoneModal.set(false);
  }

  public submitCreateMilestone(): void {
    if (!this.newMilestone.name?.trim()) return;
    const ref = this.showCreateMilestoneModal;
    this.projectService.createMilestone({
      projectId: this.projectId,
      name: this.newMilestone.name.trim(),
      description: this.newMilestone.description,
      plannedDate: this.newMilestone.plannedDate || null,
      completionPercentage: this.newMilestone.completionPercentage ?? 0
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success) {
          ref.set(false);
          this.loadAllProjectData();
        }
      }
    });
  }

  public deleteMilestone(m: Milestone): void {
    if (!confirm(`Delete milestone "${m.name}"?`)) return;
    this.projectService.deleteMilestone(m.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.loadAllProjectData()
    });
  }

  public deleteTimeEntry(entry: TimeEntry): void {
    if (!confirm(`Delete time entry for ${entry.userName || 'user'} on ${entry.workDate} (${entry.totalHours}h)?`)) return;
    this.timeTrackingService.deleteTimeEntry(entry.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.loadAllProjectData()
    });
  }

  public toggleTaskViewMode(mode: 'table' | 'grid' | 'kanban'): void {
    this.taskViewMode.set(mode);
  }

  public onFilterDropdownChange(event: Event): void {
    const val = (event.target as HTMLSelectElement).value;
    this.selectedTaskFilter.set(val);
  }

  public taskStatusOption(status: WorkItemStatus | string): void {}

  public openCreateTaskModal(): void {
    this.showCreateTaskModal.set(true);
  }

  public closeCreateTaskModal(): void {
    this.showCreateTaskModal.set(false);
  }

  public submitCreateTask(): void {
    this.workItemService.createWorkItem(this.newTask).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.closeCreateTaskModal();
          this.loadAllProjectData();
        }
      }
    });
  }

  public openCreateIssueModal(): void {
    this.showCreateIssueModal.set(true);
  }

  public closeCreateIssueModal(): void {
    this.showCreateIssueModal.set(false);
  }

  public submitCreateIssue(): void {
    this.riskService.createIssue(this.newIssue).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.closeCreateIssueModal();
          this.loadAllProjectData();
        }
      }
    });
  }

  public openUpdateIssueModal(issue: Issue): void {
    this.editingIssueId.set(issue.id);
    this.issueEditForm = {
      title: issue.title || '',
      description: issue.description || '',
      severity: issue.severity || 'MEDIUM',
      status: issue.status || 'OPEN',
      resolution: issue.resolution || '',
      classification: issue.classification || '',
      stepsToReproduce: issue.stepsToReproduce || '',
      crValue: issue.crValue ?? null,
      crManDays: issue.crManDays ?? null,
      ownerId: issue.ownerId ?? null
    };
    this.showUpdateIssueModal.set(true);
  }

  public closeUpdateIssueModal(): void {
    this.showUpdateIssueModal.set(false);
    this.editingIssueId.set(null);
  }

  public submitUpdateIssue(): void {
    const issueId = this.editingIssueId();
    if (!issueId) return;
    this.riskService.updateIssue(issueId, this.issueEditForm).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.closeUpdateIssueModal();
          this.loadAllProjectData();
        }
      }
    });
  }

  public updateIssueStatus(issue: Issue, status: string): void {
    this.riskService.updateIssue(issue.id, { status }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.loadAllProjectData()
    });
  }

  public deleteIssue(issue: Issue): void {
    if (!confirm(`Delete issue ${issue.issueCode}? This cannot be undone.`)) return;
    this.riskService.deleteIssue(issue.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        if (this.selectedIssue()?.id === issue.id) this.selectedIssue.set(null);
        this.loadAllProjectData();
      }
    });
  }

  public openLogTime(): void {
    const ref = this.dialog.open(LogTimeDialogComponent, { width: '560px' });
    ref.afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((saved: boolean) => {
      if (saved) {
        this.loadAllProjectData();
      }
    });
  }
}