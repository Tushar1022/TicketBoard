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

@Component({
  selector: 'app-project-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, MatIconModule, MatTooltipModule, MatDialogModule, MatMenuModule, DragDropModule, ResizableColumnDirective, TaskDetailDialogComponent],
  templateUrl: './project-detail.component.html',
  styleUrls: ['./project-detail.component.scss']
})
export class ProjectDetailComponent implements OnInit {
  private destroyRef = inject(DestroyRef);
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
      totalLogged: totalLogged.toFixed(1),
      plannedHours: (totalLogged * 1.1).toFixed(1),
      varianceHours: (totalLogged * 0.1).toFixed(1),
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
        this.isLoading.set(false);
      },
      error: () => {
        this.loadError.set('Failed to load project details.');
        this.isLoading.set(false);
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
    if (filter === 'overdue' || filter === 'open_tasks') {
      this.setTab('tasks');
    } else if (filter === 'total_issues' || filter === 'open_issues') {
      this.setTab('issues');
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

  private kanbanDragActive = signal<boolean>(false);

  public onKanbanDragStart(): void {
    this.kanbanDragActive.set(true);
  }

  public onKanbanDragEnd(): void {
    setTimeout(() => this.kanbanDragActive.set(false), 0);
  }

  public openKanbanDetail(item: WorkItem): void {
    if (this.kanbanDragActive()) return;
    this.openTaskDetail(item);
  }

  public getColumnItems(status: string): WorkItem[] {
    return this.filteredTasks().filter(
      (t) => this.normalizeStatus(t.status) === this.normalizeStatus(status)
    );
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
      this.updateItemStatus(movedItem, targetStatus);
    }
  }

  public updateItemStatus(item: WorkItem, newStatus: string): void {
    this.workItemService.updateStatus(item.id, newStatus as any).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.tasks.update((list) => list.map((t) => (t.id === item.id ? res.data : t)));
          if (this.selectedTask()?.id === item.id) {
            this.selectedTask.set(res.data);
          }
        }
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
    this.project.set({ ...p });
    this.projectService.updateProject(p.id, { [field]: value }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      error: (err) => console.error('Update failed:', err)
    });
    this.stopCellEdit();
  }

  public updateMilestoneField(m: Milestone, field: string, value: any): void {
    (m as any)[field] = value;
    this.stopCellEdit();
  }

  public updateTimeEntryField(entry: TimeEntry, field: string, value: any): void {
    (entry as any)[field] = value;
    this.stopCellEdit();
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

  public openLogTime(): void {
    const ref = this.dialog.open(LogTimeDialogComponent, { width: '560px' });
    ref.afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((saved: boolean) => {
      if (saved) {
        this.loadAllProjectData();
      }
    });
  }
}