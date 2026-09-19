import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnInit,
  Output,
  ViewChild,
  signal,
  inject,
  OnDestroy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { boardStatuses, statusOptions } from '../../../core/config/status.config';
import { WorkItemService } from '../../../core/services/work-item.service';
import { CommentService } from '../../../core/services/comment.service';
import { DocumentService } from '../../../core/services/document.service';
import { TimeTrackingService } from '../../../core/services/timetracking.service';
import { IssueWorkbenchComponent } from '../issue-workbench/issue-workbench.component';
import { ToastService } from '../toast/toast.service';
import {
  ActivityLog,
  Comment,
  DependencyDto,
  DependencyType,
  Project,
  TaskDocument,
  TimeEntry,
  User,
  WorkItem,
  WorkItemStatus
} from '../../../core/models/api.models';

export interface BoardColumn {
  status: string;
  label: string;
}

@Component({
  selector: 'app-task-detail-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatTooltipModule, IssueWorkbenchComponent],
  templateUrl: './task-detail-dialog.component.html',
  styleUrls: ['./task-detail-dialog.component.scss']
})
export class TaskDetailDialogComponent implements OnDestroy, OnInit, AfterViewInit {
  @Input({ required: true }) task!: WorkItem;
  @Input() users: User[] = [];
  @Input() projects: Project[] = [];

  @Output() close = new EventEmitter<void>();
  @Output() saved = new EventEmitter<void>();

  @ViewChild('descEditor') private descEditor?: ElementRef<HTMLDivElement>;

  private workItemService = inject(WorkItemService);
  private commentService = inject(CommentService);
  private documentService = inject(DocumentService);
  private timeTrackingService = inject(TimeTrackingService);
  private toastService = inject(ToastService);

  public currentTask = signal<WorkItem>(this.task);
  public editForm = signal<any>(null);
  public drawerTab = signal<'INFO' | 'SUBTASKS' | 'DOCS' | 'TIMELOGS' | 'DEPENDENCIES' | 'ISSUES' | 'COMMENTS' | 'AUDIT' | 'JIRA'>('INFO');
  public isSavingDrawer = signal<boolean>(false);
  public saveMessage = signal<string>('');

  public timerRunning = signal<boolean>(false);
  public timerSeconds = signal<number>(0);
  public timerDisplay = signal<string>('00:00:00');
  private timerInterval: any = null;

  public taskComments = signal<Comment[]>([]);
  public taskAuditLogs = signal<ActivityLog[]>([]);
  public taskDocuments = signal<TaskDocument[]>([]);
  public taskTimeEntries = signal<TimeEntry[]>([]);
  public newCommentText = signal<string>('');

  // Subtask creation form (creates a child task linked to the current parent)
  public newSubtaskTitle = signal<string>('');
  public newSubtaskDescription = signal<string>('');
  public newSubtaskHours = signal<number>(4.0);
  public newSubtaskAssigneeId = signal<number | null>(null);
  public newSubtaskDueDate = signal<string>('');
  public newSubtaskPriority = signal<string>('MEDIUM');

  // Dependency form
  public projectTasks = signal<WorkItem[]>([]);
  public newDepTargetId = signal<number | null>(null);
  public newDepType = signal<DependencyType>('BLOCKS');
  public dependencyTypes: DependencyType[] = ['BLOCKS', 'BLOCKED_BY', 'DEPENDS_ON', 'RELATED_TO', 'DUPLICATES'];

  public selectedDocFile = signal<File | null>(null);

  public readonly statusOptions = statusOptions;

  public get kanbanColumns(): BoardColumn[] {
    return boardStatuses().map((c) => ({ status: c.value, label: c.label }));
  }

  ngOnInit(): void {
    this.currentTask.set(this.task);
    this.editForm.set({ ...this.task });

    // Refresh from the backend so recorded live data (subtasks, dependencies,
    // actual hours, exit dates, etc.) is always current when the dialog opens.
    this.workItemService.getWorkItemById(this.task.id).subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.currentTask.set(res.data);
          this.editForm.set({ ...res.data });
          this.syncDescriptionEditor();
        }
        this.loadTaskDetails(this.task.id);
      }
    });
  }

  ngAfterViewInit(): void {
    this.syncDescriptionEditor();
  }

  public closeDialog(): void {
    this.stopTimer();
    this.close.emit();
  }

  public onBackdropClick(): void {
    this.closeDialog();
  }

  public onStatusChange(newStatus: string): void {
    const task = this.currentTask();
    if (!task || task.status === newStatus) return;

    const applyResult = (res: any) => {
      if (res.success && res.data) {
        this.currentTask.set(res.data);
        this.editForm.update((f) => ({ ...f, status: res.data.status }));
        this.toastService.info(`Task ${this.task.ticketNumber} status changed to ${newStatus.replace(/_/g, ' ')}.`);
        this.saveMessage.set('Status updated');
        this.saved.emit();
      }
    };

    if (newStatus === 'BLOCKED') {
      this.workItemService
        .blockWorkItem(task.id, {
          reason: 'Blocked — awaiting dependency or decision',
          owner: 'System',
          expectedResolutionDate: new Date(Date.now() + 86400000 * 2).toISOString().substring(0, 10)
        })
        .subscribe({ next: applyResult });
    } else {
      this.workItemService.updateStatus(task.id, newStatus as WorkItemStatus).subscribe({ next: applyResult });
    }
  }

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

  public saveTaskChanges(): void {
    const task = this.currentTask();
    const form = this.editForm();
    if (!task || !form) return;

    this.isSavingDrawer.set(true);
    this.saveMessage.set('');
    this.workItemService.updateWorkItem(task.id, this.buildUpdatePayload(form)).subscribe({
      next: (res: any) => {
        this.isSavingDrawer.set(false);
        if (res.success && res.data) {
          this.currentTask.set(res.data);
          this.editForm.set({ ...res.data });
          this.syncDescriptionEditor();
          this.toastService.success(`Task ${this.task.ticketNumber} changes were saved.`);
          this.saveMessage.set('Saved');
          this.saved.emit();
        }
      },
      error: () => {
        this.isSavingDrawer.set(false);
        this.toastService.error('Failed to save task changes.');
        this.saveMessage.set('Save failed');
      }
    });
  }

  public saveDrawerField(field: string, value: any): void {
    const form = this.editForm();
    if (!form) return;
    if (form[field] === value) return;

    this.editForm.update((f) => ({ ...f, [field]: value }));
    this.saveTaskChanges();
  }

  public loadTaskDetails(taskId: number): void {
    const task = this.currentTask();
    this.taskTimeEntries.set([]);

    this.commentService.getComments('WORK_ITEM', taskId).subscribe({
      next: (res: any) => {
        if (res.success && res.data) this.taskComments.set(res.data);
      }
    });

    this.commentService.getAuditLogs('WORK_ITEM', taskId).subscribe({
      next: (res: any) => {
        if (res.success && res.data) this.taskAuditLogs.set(res.data);
      }
    });

    this.workItemService.getDocuments(taskId).subscribe({
      next: (res: any) => {
        if (res.success && res.data) this.taskDocuments.set(res.data);
      }
    });

    if (task && task.projectId) {
      this.timeTrackingService.getTimeEntriesByProject(task.projectId).subscribe({
        next: (res: any) => {
          if (res.success && res.data) {
            this.taskTimeEntries.set(
              res.data.filter(
                (e: TimeEntry) => e.workItemId === taskId || e.workItemTicketNumber === task.ticketNumber
              )
            );
          }
        }
      });
    }
  }

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

  public triggerJiraSync(): void {
    const task = this.currentTask();
    if (!task) return;

    this.workItemService.syncWithJira(task.id).subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.currentTask.set(res.data);
          this.editForm.set({ ...res.data });
          this.toastService.success('Task synchronized with JIRA successfully.');
          this.saved.emit();
        }
      },
      error: () => this.toastService.error('JIRA synchronization failed.')
    });
  }

  public postComment(): void {
    const task = this.currentTask();
    if (!task || !this.newCommentText().trim()) return;

    this.commentService
      .addComment({ entityType: 'WORK_ITEM', entityId: task.id, content: this.newCommentText() })
      .subscribe({
        next: (res: any) => {
          if (res.success) {
            this.toastService.success('Comment added to the task.');
            this.newCommentText.set('');
            this.loadTaskDetails(task.id);
          }
        },
        error: () => this.toastService.error('Failed to add comment.')
      });
  }

  public onDocFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length ? input.files[0] : null;
    this.selectedDocFile.set(file);
  }

  public uploadDocument(): void {
    const task = this.currentTask();
    const file = this.selectedDocFile();
    if (!task || !file) return;

    this.workItemService.uploadDocument(task.id, file).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.toastService.success('Document uploaded and attached to the task.');
          this.selectedDocFile.set(null);
          this.loadTaskDetails(task.id);
          this.saved.emit();
        }
      },
      error: () => this.toastService.error('Document upload failed.')
    });
  }

  public downloadDoc(doc: TaskDocument): void {
    const url = doc.downloadUrl || doc.fileUrl;
    if (url) {
      this.documentService.openDownload(url);
    }
  }

  public deleteDoc(docId: number): void {
    const task = this.currentTask();
    if (!task) return;

    this.workItemService.deleteDocument(docId).subscribe({
      next: () => {
        this.toastService.success('Document removed from the task.');
        this.loadTaskDetails(task.id);
      },
      error: () => this.toastService.error('Failed to delete the document.')
    });
  }

  public unblockTask(): void {
    const task = this.currentTask();
    if (!task) return;

    this.workItemService.unblockWorkItem(task.id, { reason: 'Resolved by team engineer' }).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.toastService.success('Task unblocked successfully.');
          this.refreshCurrentTask(res.data);
          this.saved.emit();
        }
      },
      error: () => this.toastService.error('Failed to unblock the task.')
    });
  }

  /* ── Subtask form ── */

  public addSubtask(): void {
    const task = this.currentTask();
    if (!task || !this.newSubtaskTitle().trim()) return;

    this.workItemService
      .createWorkItem({
        title: this.newSubtaskTitle().trim(),
        description: this.newSubtaskDescription().trim() || '',
        type: 'SUBTASK',
        status: 'TODO',
        priority: this.newSubtaskPriority() || task.priority,
        severity: task.severity,
        projectId: task.projectId,
        parentTaskId: task.id,
        assigneeId: this.newSubtaskAssigneeId() || null,
        dueDate: this.newSubtaskDueDate() || null,
        estimatedHours: this.newSubtaskHours(),
        startDate: new Date().toISOString().substring(0, 10)
      })
      .subscribe({
        next: (res: any) => {
          if (res.success) {
            this.toastService.success('Subtask created successfully.');
            this.newSubtaskTitle.set('');
            this.newSubtaskDescription.set('');
            this.newSubtaskHours.set(4.0);
            this.newSubtaskAssigneeId.set(null);
            this.newSubtaskDueDate.set('');
            this.newSubtaskPriority.set('MEDIUM');
            this.refreshCurrentTask();
            this.saved.emit();
          }
        },
        error: (err: any) => {
          this.toastService.error('Failed to create subtask — please try again.');
          this.saveMessage.set('Failed to create subtask — please try again.');
          setTimeout(() => this.saveMessage.set(''), 6000);
        }
      });
  }

  public depTypeLabel(type: string): string {
    return type
      .toLowerCase()
      .split('_')
      .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
      .join(' ');
  }

  /* ── Dependency form ── */

  public loadProjectTasks(): void {
    const task = this.currentTask();
    if (!task) return;
    this.workItemService.getWorkItems({ projectId: task.projectId }).subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.projectTasks.set(res.data.filter((t: WorkItem) => t.id !== task.id));
        }
      }
    });
  }

  public addDependency(): void {
    const task = this.currentTask();
    const targetId = this.newDepTargetId();
    if (!task || !targetId) return;

    this.workItemService
      .addDependency({
        sourceItemId: task.id,
        targetItemId: targetId,
        dependencyType: this.newDepType()
      })
      .subscribe({
        next: (res: any) => {
          if (res.success) {
            this.toastService.success('Dependency added to the task.');
            this.newDepTargetId.set(null);
            this.refreshCurrentTask();
            this.saved.emit();
          }
        },
        error: () => this.toastService.error('Failed to add dependency.')
      });
  }

  public removeDependency(depId: number | undefined): void {
    if (!depId) return;
    this.workItemService.deleteDependency(depId).subscribe({
      next: () => {
        this.toastService.success('Dependency removed.');
        this.refreshCurrentTask();
        this.saved.emit();
      },
      error: () => this.toastService.error('Failed to remove dependency.')
    });
  }

  /* ── Date helpers ── */

  public isPastDate(value?: string | null): boolean {
    if (!value) return false;
    const today = new Date().toISOString().substring(0, 10);
    return value < today;
  }

  /* ── Description rich-text editor (bold / italic / underline / lists) ── */

  public execFormat(command: string): void {
    document.execCommand(command, false);
    this.syncDescriptionEditor();
  }

  public clearFormatting(): void {
    document.execCommand('removeFormat', false);
    document.execCommand('removeFormat', false); // pass through once for inline elements
    this.syncDescriptionEditor();
  }

  public onDescInput(event: Event): void {
    const el = event.target as HTMLElement;
    this.editForm.update((f) => ({ ...f, description: el.innerText }));
  }

  private syncDescriptionEditor(): void {
    const el = this.descEditor?.nativeElement;
    if (el) {
      el.innerText = this.editForm()?.description || '';
    }
  }

  /* ── Internal ── */

  private refreshCurrentTask(override?: WorkItem): void {
    const taskId = this.currentTask().id;
    if (override) {
      this.currentTask.set(override);
      this.editForm.set({ ...override });
      this.syncDescriptionEditor();
      return;
    }
    this.workItemService.getWorkItemById(taskId).subscribe((res: any) => {
      if (res.success && res.data) {
        this.currentTask.set(res.data);
        this.editForm.set({ ...res.data });
        this.syncDescriptionEditor();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.timerInterval) { clearInterval(this.timerInterval); }
  }
}