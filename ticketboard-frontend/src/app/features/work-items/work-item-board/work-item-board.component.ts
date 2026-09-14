import { Component, OnInit, signal, DestroyRef, inject, OnDestroy } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { DragDropModule, CdkDragDrop, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { ResizableColumnDirective } from '../../../core/directives/resizable-column.directive';
import { TaskDetailDialogComponent } from '../../../shared/components/task-detail-dialog/task-detail-dialog.component';
import { boardStatuses, statusOptions, statusBadge, statusLabel } from '../../../core/config/status.config';
import { WorkItemService } from '../../../core/services/work-item.service';
import { ProjectService } from '../../../core/services/project.service';
import { CommentService } from '../../../core/services/comment.service';
import { DocumentService } from '../../../core/services/document.service';
import { AuthService } from '../../../core/services/auth.service';
import { TimeTrackingService } from '../../../core/services/timetracking.service';
import { RiskService } from '../../../core/services/risk.service';
import { UserService } from '../../../core/services/user.service';
import {
  ActivityLog,
  Comment,
  Issue,
  Project,
  TaskDocument,
  TimeEntry,
  User,
  WorkItem,
  WorkItemPriority,
  WorkItemSeverity,
  WorkItemStatus,
  WorkItemType
} from '../../../core/models/api.models';

@Component({
  selector: 'app-work-item-board',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatIconModule,
    MatTooltipModule,
    MatMenuModule,
    DragDropModule,
    ResizableColumnDirective,
    TaskDetailDialogComponent
  ],
  templateUrl: './work-item-board.component.html',
  styleUrls: ['./work-item-board.component.scss']
})
export class WorkItemBoardComponent implements OnDestroy, OnInit {
  private destroyRef = inject(DestroyRef);
  public workItems = signal<WorkItem[]>([]);
  public filteredWorkItems = signal<WorkItem[]>([]);
  public projects = signal<Project[]>([]);
  public isLoading = signal<boolean>(true);

  // View Mode: LIST, KANBAN, GANTT
  public viewMode = signal<'LIST' | 'KANBAN' | 'GANTT'>('LIST');

  // Inline Cell Editing State
  public inlineEditing = signal<{ id: number; field: string } | null>(null);

  // Quick Filter Tabs & Dropdown View
  public activeQuickFilter = signal<string>('ALL_OPEN');

  // Group By
  public groupBy = signal<'NONE' | 'STATUS' | 'PROJECT' | 'ASSIGNEE' | 'PRIORITY' | 'BILLING'>('NONE');

  // Search & Select Filters
  public searchQuery = signal<string>('');
  public selectedProjectId = signal<number | null>(null);
  public selectedStatus = signal<string>('ALL');
  public selectedPriority = signal<string>('ALL');
  public selectedAssignee = signal<string>('ALL');

  // Kanban Columns (Generic Status Catalog — add statuses in core/config/status.config.ts)
  public get kanbanColumns(): { status: WorkItemStatus; label: string; badgeClass: string }[] {
    return boardStatuses().map((c) => ({ status: c.value, label: c.label, badgeClass: c.badgeClass }));
  }

  public statusOptions = statusOptions;
  public statusBadge = statusBadge;
  public statusLabel = statusLabel;

  // Detailed Task Drawer State
  public selectedTask = signal<WorkItem | null>(null);
  public editForm = signal<any>(null);
  public drawerTab = signal<'INFO' | 'JIRA' | 'SUBTASKS' | 'TIMELOGS' | 'DOCS' | 'DEPENDENCIES' | 'ISSUES' | 'COMMENTS' | 'AUDIT'>('INFO');
  public users = signal<User[]>([]);
  public isSavingDrawer = signal<boolean>(false);
  public saveMessage = signal<string>('');

  // Edge-triggered inline cell editing (shared with table rows)
  // Stopwatch / Timer State
  public timerRunning = signal<boolean>(false);
  public timerSeconds = signal<number>(0);
  public timerDisplay = signal<string>('00:00:00');
  private timerInterval: any = null;

  // Comments and Audit in Drawer
  public taskComments = signal<Comment[]>([]);
  public taskAuditLogs = signal<ActivityLog[]>([]);
  public taskDocuments = signal<TaskDocument[]>([]);
  public taskTimeEntries = signal<TimeEntry[]>([]);
  public taskIssues = signal<Issue[]>([]);
  public newCommentText = signal<string>('');

  // Subtask Input State
  public newSubtaskTitle = signal<string>('');
  public newSubtaskHours = signal<number>(4.0);

  // Document Upload State
  public selectedDocFile = signal<File | null>(null);

  // Create Task Modal State
  public showCreateModal = signal<boolean>(false);
  public newTask: any = {
    ticketNumber: '',
    title: '',
    description: '',
    type: 'TASK' as WorkItemType,
    priority: 'HIGH' as WorkItemPriority,
    severity: 'MEDIUM' as WorkItemSeverity,
    status: 'TODO' as WorkItemStatus,
    projectId: null,
    associatedTeam: '',
    estimatedHours: 40.0,
    devEffortDays: 3.0,
    qcEffortDays: 3.0,
    durationDays: 14,
    billingType: 'Billable',
    startDate: new Date().toISOString().substring(0, 10),
    dueDate: new Date(Date.now() + 86400000 * 14).toISOString().substring(0, 10)
  };

  // Blocker Dialog State
  public showBlockerModal = signal<boolean>(false);
  public blockerReason = signal<string>('');
  public blockerOwner = signal<string>('');
  public blockerExpectedDate = signal<string>('');

  constructor(
    private workItemService: WorkItemService,
    private projectService: ProjectService,
    private commentService: CommentService,
    private documentService: DocumentService,
    private timeTrackingService: TimeTrackingService,
    private riskService: RiskService,
    private userService: UserService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadProjects();
    this.loadUsers();
    this.loadWorkItems();
  }

  public loadUsers(): void {
    this.userService.getUsers({ status: 'ACTIVE' }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.users.set(res.data);
        }
      }
    });
  }

  public loadProjects(): void {
    this.projectService.getAllProjects().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.projects.set(res.data);
          if (res.data.length > 0 && !this.newTask.projectId) {
            this.newTask.projectId = res.data[0].id;
          }
        }
      }
    });
  }

  public loadWorkItems(): void {
    this.isLoading.set(true);
    const params: any = {};
    if (this.selectedProjectId()) params.projectId = this.selectedProjectId();

    this.workItemService.getWorkItems(params).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        this.isLoading.set(false);
        if (res.success && res.data) {
          this.workItems.set(res.data);
          this.applyFilters();
        }
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  public setQuickFilter(filter: string): void {
    this.activeQuickFilter.set(filter);
    this.applyFilters();
  }

  public setViewMode(mode: 'LIST' | 'KANBAN' | 'GANTT'): void {
    this.viewMode.set(mode);
  }

  public setGroupBy(group: 'NONE' | 'STATUS' | 'PROJECT' | 'ASSIGNEE' | 'PRIORITY' | 'BILLING'): void {
    this.groupBy.set(group);
  }

  public applyFilters(): void {
    let items = [...this.workItems()];

    // Quick Filters
    const qf = this.activeQuickFilter();
    if (qf === 'ALL_OPEN') {
      items = items.filter((i) => i.status !== 'COMPLETED' && i.status !== 'CLOSED');
    } else if (qf === 'MY_OPEN') {
      const myId = this.authService.currentUser()?.id;
      items = items.filter((i) => i.assigneeId === myId && i.status !== 'COMPLETED' && i.status !== 'CLOSED');
    } else if (qf === 'DELAYED') {
      const now = new Date().toISOString().substring(0, 10);
      items = items.filter((i) => i.dueDate && i.dueDate < now && i.status !== 'COMPLETED');
    } else if (qf === 'HIGH_PRIORITY') {
      items = items.filter((i) => i.priority === 'HIGH' || i.priority === 'CRITICAL');
    } else if (qf === 'BLOCKED') {
      items = items.filter((i) => i.status === 'BLOCKED');
    } else if (qf === 'UAT_EXIT') {
      items = items.filter((i) => i.status === 'UAT_EXIT');
    } else if (qf === 'SIT_EXIT') {
      items = items.filter((i) => i.status === 'SIT_EXIT');
    } else if (qf === 'COMPLETED') {
      items = items.filter((i) => i.status === 'COMPLETED' || i.status === 'CLOSED');
    }

    // Dropdown Filters
    if (this.selectedProjectId()) {
      items = items.filter((i) => i.projectId === this.selectedProjectId());
    }
    if (this.selectedStatus() !== 'ALL') {
      items = items.filter((i) => i.status === this.selectedStatus());
    }
    if (this.selectedPriority() !== 'ALL') {
      items = items.filter((i) => i.priority === this.selectedPriority());
    }

    // Search Query
    if (this.searchQuery().trim()) {
      const q = this.searchQuery().toLowerCase();
      items = items.filter(
        (i) =>
          i.ticketNumber.toLowerCase().includes(q) ||
          i.title.toLowerCase().includes(q) ||
          (i.assigneeName && i.assigneeName.toLowerCase().includes(q)) ||
          (i.tags && i.tags.toLowerCase().includes(q))
      );
    }

    this.filteredWorkItems.set(items);
  }

  // Kanban Column Helpers
  public getColumnItems(status: WorkItemStatus): WorkItem[] {
    return this.filteredWorkItems().filter((item) => item.status === status);
  }

  public getConnectedDropLists(): string[] {
    return this.kanbanColumns.map((col) => 'cdk-drop-list-' + col.status);
  }

  // Distinguish a genuine drag from a simple click so cards only open when clicked
  private kanbanDragActive = signal<boolean>(false);

  public onKanbanDragStart(): void {
    this.kanbanDragActive.set(true);
  }

  public onKanbanDragEnd(): void {
    setTimeout(() => this.kanbanDragActive.set(false), 0);
  }

  public openKanbanDetail(item: WorkItem): void {
    if (this.kanbanDragActive()) return;
    this.openTaskDrawer(item);
  }

  public onKanbanDrop(event: CdkDragDrop<WorkItem[], any>, targetStatus: WorkItemStatus): void {
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

  public updateItemStatus(item: WorkItem, newStatus: WorkItemStatus): void {
    if (newStatus === 'BLOCKED') {
      this.openBlockerModal(item);
      return;
    }

    this.workItemService.updateStatus(item.id, newStatus).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.loadWorkItems();
          if (this.selectedTask() && this.selectedTask()!.id === item.id) {
            this.selectedTask.set(res.data);
            if (this.editForm()) {
              this.editForm.update((f) => ({ ...f, status: res.data.status }));
            }
          }
        }
      }
    });
  }

  // Detail Drawer Actions
  public openTaskDrawer(task: WorkItem): void {
    this.selectedTask.set(task);
  }

  public closeTaskDrawer(): void {
    this.selectedTask.set(null);
  }

  public onTaskModalSaved(): void {
    this.loadWorkItems();
  }

  // Builds a full WorkItemCreateDto payload so PUT validation (title + projectId)
  // always passes even though we send it from partial / blur-triggered edits.
  private buildUpdatePayload(form: any): any {
    return {
      ticketNumber: form.ticketNumber,
      title: form.title,
      description: form.description || '',
      type: form.type,
      priority: form.priority,
      severity: form.severity,
      status: form.status,
      projectId: form.projectId != null ? Number(form.projectId) : null,
      requirementId: form.requirementId || null,
      parentTaskId: form.parentTaskId || null,
      assigneeId: form.assigneeId != null ? Number(form.assigneeId) : null,
      reporterId: form.reporterId || null,
      estimatedHours: form.estimatedHours != null ? Number(form.estimatedHours) : null,
      startDate: form.startDate || null,
      dueDate: form.dueDate || null,
      devExitDate: form.devExitDate || null,
      sitExitDate: form.sitExitDate || null,
      uatExitDate: form.uatExitDate || null,
      sdDeliveryDate: form.sdDeliveryDate || null,
      goLiveDate: form.goLiveDate || null,
      devEffortDays: form.devEffortDays != null ? Number(form.devEffortDays) : null,
      qcEffortDays: form.qcEffortDays != null ? Number(form.qcEffortDays) : null,
      durationDays: form.durationDays != null ? Number(form.durationDays) : null,
      completionPercentage: form.completionPercentage != null ? Number(form.completionPercentage) : null,
      billingType: form.billingType || 'None',
      associatedTeam: form.associatedTeam || '',
      jiraTaskId: form.jiraTaskId || null,
      jiraStatus: form.jiraStatus || null,
      tags: form.tags || '',
      reminder: form.reminder || '',
      recurrence: form.recurrence || '',
      labels: form.labels || ''
    };
  }

  // Explicit Save button in modal footer — sends the whole form as a PUT.
  public saveTaskChanges(): void {
    const task = this.selectedTask();
    const form = this.editForm();
    if (!task || !form) return;

    this.isSavingDrawer.set(true);
    this.saveMessage.set('');
    this.workItemService.updateWorkItem(task.id, this.buildUpdatePayload(form)).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        this.isSavingDrawer.set(false);
        if (res.success && res.data) {
          this.selectedTask.set(res.data);
          this.editForm.set({ ...res.data });
          this.saveMessage.set('Saved');
          this.loadWorkItems();
        }
      },
      error: () => {
        this.isSavingDrawer.set(false);
        this.saveMessage.set('Save failed');
      }
    });
  }

  // Drawer inline editing — applies the edit locally then saves the full object.
  public saveDrawerField(field: string, value: any): void {
    const task = this.selectedTask();
    const form = this.editForm();
    if (!task || !form) return;
    if ((task as any)[field] === value) return;

    this.editForm.update((f) => ({ ...f, [field]: value }));
    this.saveTaskChanges();
  }

  public loadDrawerDetails(taskId: number): void {
    const task = this.selectedTask();
    this.taskTimeEntries.set([]);
    this.taskIssues.set([]);

    this.commentService.getComments('WORK_ITEM', taskId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.taskComments.set(res.data);
        }
      }
    });

    this.commentService.getAuditLogs('WORK_ITEM', taskId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.taskAuditLogs.set(res.data);
        }
      }
    });

    this.workItemService.getDocuments(taskId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.taskDocuments.set(res.data);
        }
      }
    });

    if (task && task.projectId) {
      // Load time entries logged against this work item (filtered from project feed)
      this.timeTrackingService.getTimeEntriesByProject(task.projectId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: (res: any) => {
          if (res.success && res.data) {
            this.taskTimeEntries.set(
              res.data.filter((e: TimeEntry) => e.workItemId === taskId || e.workItemTicketNumber === task.ticketNumber)
            );
          }
        }
      });

      // Load issues linked to this work item via its ticket number
      this.riskService.getIssuesByProject(task.projectId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: (res: any) => {
          if (res.success && res.data) {
            this.taskIssues.set(
              res.data.filter(
                (i: Issue) => i.linkedTaskIds && i.linkedTaskIds.includes(task.ticketNumber)
              )
            );
          }
        }
      });
    }
  }

  // Grouped list sections for LIST view
  public getGroupGroups(): { key: string; items: WorkItem[] }[] {
    const gb = this.groupBy();
    if (gb === 'NONE') {
      return [{ key: '', items: this.filteredWorkItems() }];
    }
    const map = new Map<string, WorkItem[]>();
    for (const item of this.filteredWorkItems()) {
      let key = '';
      if (gb === 'STATUS') key = this.statusLabel(item.status);
      else if (gb === 'PROJECT') key = item.projectName || 'Unassigned Project';
      else if (gb === 'ASSIGNEE') key = item.assigneeName || 'Unassigned';
      else if (gb === 'PRIORITY') key = item.priority || 'Undefined';
      else if (gb === 'BILLING') key = item.billingType || 'None';
      if (!map.has(key)) map.set(key, []);
      map.get(key)!.push(item);
    }
    return Array.from(map.entries()).map(([key, items]) => ({ key, items }));
  }

  // Stopwatch / Timer Controls
  public toggleTimer(): void {
    if (this.timerRunning()) {
      this.stopTimer();
    } else {
      this.startTimer();
    }
  }

  private startTimer(): void {
    this.timerRunning.set(true);
    this.timerInterval = setInterval(() => {
      this.timerSeconds.update((s) => s + 1);
      const total = this.timerSeconds();
      const hrs = String(Math.floor(total / 3600)).padStart(2, '0');
      const mins = String(Math.floor((total % 3600) / 60)).padStart(2, '0');
      const secs = String(total % 60).padStart(2, '0');
      this.timerDisplay.set(`${hrs}:${mins}:${secs}`);
    }, 1000);
  }

  private stopTimer(): void {
    this.timerRunning.set(false);
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }

  public resetTimer(): void {
    this.stopTimer();
    this.timerSeconds.set(0);
    this.timerDisplay.set('00:00:00');
  }

  // Jira 1-Click Sync
  public triggerJiraSync(): void {
    const task = this.selectedTask();
    if (!task) return;

    this.workItemService.syncWithJira(task.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.selectedTask.set(res.data);
          this.loadWorkItems();
          this.loadDrawerDetails(task.id);
        }
      }
    });
  }

  // Comments
  public postComment(): void {
    const task = this.selectedTask();
    if (!task || !this.newCommentText().trim()) return;

    this.commentService
      .addComment({
        entityType: 'WORK_ITEM',
        entityId: task.id,
        content: this.newCommentText()
      })
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: (res: any) => {
          if (res.success) {
            this.newCommentText.set('');
            this.loadDrawerDetails(task.id);
          }
        }
      });
  }

  // Document Upload in Drawer
  public onDocFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length ? input.files[0] : null;
    this.selectedDocFile.set(file);
  }

  public uploadDocument(): void {
    const task = this.selectedTask();
    const file = this.selectedDocFile();
    if (!task || !file) return;

    this.workItemService.uploadDocument(task.id, file).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.selectedDocFile.set(null);
          this.loadDrawerDetails(task.id);
          this.loadWorkItems();
        }
      }
    });
  }

  public downloadDoc(doc: TaskDocument): void {
    const url = doc.downloadUrl || doc.fileUrl;
    if (url) {
      this.documentService.openDownload(url);
    }
  }

  public deleteDoc(docId: number): void {
    const task = this.selectedTask();
    if (!task) return;

    this.workItemService.deleteDocument(docId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.loadDrawerDetails(task.id);
      }
    });
  }

  // Subtask Creation
  public addSubtask(): void {
    const task = this.selectedTask();
    if (!task || !this.newSubtaskTitle().trim()) return;

    this.workItemService
      .createWorkItem({
        title: this.newSubtaskTitle(),
        type: 'SUBTASK',
        status: 'TODO',
        priority: task.priority,
        severity: task.severity,
        projectId: task.projectId,
        parentTaskId: task.id,
        estimatedHours: this.newSubtaskHours(),
        startDate: new Date().toISOString().substring(0, 10)
      })
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: (res: any) => {
          if (res.success) {
            this.newSubtaskTitle.set('');
            this.newSubtaskHours.set(4.0);
            if (this.selectedTask()?.id === task.id) {
              this.workItemService.getWorkItemById(task.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe((r: any) => {
                if (r.success) this.selectedTask.set(r.data);
              });
            }
            this.loadWorkItems();
          }
        }
      });
  }

  // Create Task Modal
  public openCreateModal(): void {
    this.showCreateModal.set(true);
  }

  public closeCreateModal(): void {
    this.showCreateModal.set(false);
  }

  public submitCreateTask(): void {
    this.workItemService.createWorkItem(this.newTask).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.closeCreateModal();
          this.loadWorkItems();
        }
      }
    });
  }

  // Blocker Modal
  public openBlockerModal(item: WorkItem): void {
    this.selectedTask.set(item);
    this.blockerReason.set('');
    this.blockerExpectedDate.set(new Date(Date.now() + 86400000 * 2).toISOString().substring(0, 10));
    this.showBlockerModal.set(true);
  }

  public closeBlockerModal(): void {
    this.showBlockerModal.set(false);
  }

  public confirmBlocker(): void {
    const task = this.selectedTask();
    if (!task) return;

    this.workItemService
      .blockWorkItem(task.id, {
        reason: this.blockerReason(),
        owner: this.blockerOwner(),
        expectedResolutionDate: this.blockerExpectedDate()
      })
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: (res: any) => {
          if (res.success) {
            this.closeBlockerModal();
            this.loadWorkItems();
          }
        }
      });
  }

  public startInlineEdit(id: number, field: string, event?: MouseEvent): void {
    if (event) event.stopPropagation();
    this.inlineEditing.set({ id, field });
  }

  public stopInlineEdit(): void {
    this.inlineEditing.set(null);
  }

  public isEditingCell(id: number, field: string): boolean {
    const cur = this.inlineEditing();
    return !!cur && cur.id === id && cur.field === field;
  }

  public updateField(task: WorkItem, field: string, value: any): void {
    (task as any)[field] = value;
    this.workItemService.updateWorkItem(task.id, this.buildUpdatePayload(task)).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      error: (err) => console.error('Update failed:', err)
    });
    this.stopInlineEdit();
  }

  public unblockTask(task: WorkItem): void {
    this.workItemService.unblockWorkItem(task.id, { reason: 'Resolved by team engineer' }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.loadWorkItems();
          if (this.selectedTask()?.id === task.id) {
            this.selectedTask.set(res.data);
          }
        }
      }
    });
  }

  ngOnDestroy(): void {
    if (this.timerInterval) { clearInterval(this.timerInterval); }
  }
}