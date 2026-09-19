import {
  Component,
  DestroyRef,
  EventEmitter,
  Input,
  OnInit,
  Output,
  ViewEncapsulation,
  computed,
  inject,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { forkJoin } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { lookupColor, lookupLabel } from '../../utils/lookup.utils';
import { RiskService } from '../../../core/services/risk.service';
import { LookupDataService } from '../../../core/services/lookup-data.service';
import { UserService } from '../../../core/services/user.service';
import { ProjectService } from '../../../core/services/project.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../toast/toast.service';
import {
  Issue,
  IssueComment,
  IssueHistory,
  IssueWatcher,
  LookupData,
  Milestone,
  User
} from '../../../core/models/api.models';

@Component({
  selector: 'app-issue-workbench',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  templateUrl: './issue-workbench.component.html',
  styleUrls: ['./issue-workbench.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class IssueWorkbenchComponent implements OnInit {
  @Input({ required: true }) projectId!: number;
  @Input() taskCode?: string;

  @Output() changed = new EventEmitter<void>();

  private destroyRef = inject(DestroyRef);
  private riskService = inject(RiskService);
  private lookupDataService = inject(LookupDataService);
  private userService = inject(UserService);
  private projectService = inject(ProjectService);
  public authService = inject(AuthService);
  private toastService = inject(ToastService);

  // ── Lookups ──
  public lookupMap = signal<Record<string, LookupData[]>>({});
  public issueStatuses = computed(() => this.lookupMap()['ISSUE_STATUS'] || []);
  public issueSeverities = computed(() => this.lookupMap()['ISSUE_SEVERITY'] || []);
  public issueClassifications = computed(() => this.lookupMap()['ISSUE_CLASSIFICATION'] || []);
  public priorities = computed(() => this.lookupMap()['PRIORITY'] || []);

  public lookupLabel(category: string, value: string | null | undefined, fallback = '—'): string {
    return lookupLabel(this.lookupMap()[category], value, fallback);
  }

  public lookupColor(category: string, value: string | null | undefined, fallback = '#64748B'): string {
    return lookupColor(this.lookupMap()[category], value, fallback);
  }

  // ── Reference data ──
  public users = signal<User[]>([]);
  public milestones = signal<Milestone[]>([]);

  // ── Issues list + filters ──
  public issues = signal<Issue[]>([]);
  public loading = signal<boolean>(true);
  public issueFilterStatus = signal<string>('ALL');
  public issueFilterSeverity = signal<string>('ALL');
  public issueSearch = signal<string>('');

  public filteredIssues = computed(() => {
    const list = this.issues();
    const q = this.issueSearch().trim().toLowerCase();
    return list.filter((iss: Issue) => {
      if (this.taskCode && !(iss.linkedTaskIds || []).includes(this.taskCode)) return false;
      if (this.issueFilterStatus() !== 'ALL' && iss.status !== this.issueFilterStatus()) return false;
      if (this.issueFilterSeverity() !== 'ALL' && iss.severity !== this.issueFilterSeverity()) return false;
      if (q) {
        const hay = `${iss.issueCode} ${iss.title} ${iss.description} ${iss.reporterName} ${iss.assigneeName}`.toLowerCase();
        if (!hay.includes(q)) return false;
      }
      return true;
    });
  });

  public statusCount(status: string): number {
    const code = this.taskCode;
    const scope = code ? this.issues().filter((i) => (i.linkedTaskIds || []).includes(code)) : this.issues();
    return scope.filter((i) => i.status === status).length;
  }

  // ── Selected issue detail ──
  public selectedIssue = signal<Issue | null>(null);
  public issueDetailTab = signal<string>('overview');
  public issueComments = signal<IssueComment[]>([]);
  public issueHistory = signal<IssueHistory[]>([]);
  public issueWatchers = signal<IssueWatcher[]>([]);
  public newIssueComment = signal<string>('');
  public issueDetailLoading = signal<boolean>(false);

  // ── Create / edit modal state ──
  public showCreateIssueModal = signal<boolean>(false);
  public showUpdateIssueModal = signal<boolean>(false);
  public submitting = signal<boolean>(false);
  public editingIssueId = signal<number | null>(null);

  public newIssue: any = {
    title: '',
    description: '',
    severity: 'MEDIUM',
    status: 'OPEN',
    classification: '',
    category: '',
    priority: 'MEDIUM',
    reporterId: null,
    assigneeId: null,
    milestoneId: null,
    stepsToReproduce: '',
    expectedBehavior: '',
    actualBehavior: '',
    acceptanceCriteria: ''
  };

  public issueEditForm: any = {};

  private readonly issueTransitions: Record<string, string[]> = {
    OPEN: ['IN_PROGRESS', 'RESOLVED', 'CLOSED'],
    IN_PROGRESS: ['RESOLVED', 'CLOSED'],
    RESOLVED: ['CLOSED', 'REOPENED'],
    CLOSED: ['REOPENED'],
    REOPENED: ['IN_PROGRESS', 'RESOLVED', 'CLOSED']
  };

  public allowedStatusTransitions(status: string | undefined): string[] {
    return this.issueTransitions[status || 'OPEN'] || [];
  }

  ngOnInit(): void {
    this.loadLookups();
    this.loadUsers();
    this.loadMilestones();
    this.loadIssues();
  }

  public loadLookups(): void {
    this.lookupDataService.getAll().subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          const map: Record<string, LookupData[]> = {};
          for (const l of res.data) {
            if (!map[l.category]) map[l.category] = [];
            if (l.isActive !== false) map[l.category].push(l);
          }
          this.lookupMap.set(map);
        }
      }
    });
  }

  public loadUsers(): void {
    this.userService.getUsers({ status: 'ACTIVE' }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success && res.data) this.users.set(res.data);
      }
    });
  }

  public loadMilestones(): void {
    this.projectService.getProjectById(this.projectId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success && res.data?.milestones) this.milestones.set(res.data.milestones);
      }
    });
  }

  public loadIssues(): void {
    this.loading.set(true);
    this.riskService.getIssuesByProject(this.projectId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success && res.data) this.issues.set(res.data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  public selectIssue(issue: Issue): void {
    this.selectedIssue.set(issue);
    this.issueDetailTab.set('overview');
    this.loadIssueDetail(issue.id);
  }

  public loadIssueDetail(issueId: number): void {
    this.issueDetailLoading.set(true);
    forkJoin({
      comments: this.riskService.getIssueComments(issueId),
      history: this.riskService.getIssueHistory(issueId),
      watchers: this.riskService.getIssueWatchers(issueId)
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.comments.success && res.comments.data) this.issueComments.set(res.comments.data);
        if (res.history.success && res.history.data) this.issueHistory.set(res.history.data);
        if (res.watchers.success && res.watchers.data) this.issueWatchers.set(res.watchers.data);
        this.issueDetailLoading.set(false);
      },
      error: () => this.issueDetailLoading.set(false)
    });
  }

  public addIssueComment(): void {
    const issue = this.selectedIssue();
    const content = this.newIssueComment().trim();
    const authorId = this.authService.currentUser()?.id;
    if (!issue || !content || !authorId) return;
    this.riskService.addIssueComment(issue.id, { authorId, content }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.toastService.success('Comment added to the issue.');
          this.newIssueComment.set('');
          this.loadIssueDetail(issue.id);
          this.changed.emit();
        }
      },
      error: () => this.toastService.error('Failed to add comment.')
    });
  }

  public isWatchingIssue(userId?: number): boolean {
    if (!userId) return false;
    return this.issueWatchers().some((w) => w.userId === userId);
  }

  public toggleWatcher(): void {
    const issue = this.selectedIssue();
    const authId = this.authService.currentUser()?.id;
    if (!issue || !authId) return;
    if (this.isWatchingIssue(authId)) {
      this.riskService.removeIssueWatcher(issue.id, authId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: () => {
          this.toastService.info('You are no longer watching this issue.');
          this.loadIssueDetail(issue.id);
        },
        error: () => this.toastService.error('Failed to update watcher.')
      });
    } else {
      this.riskService.addIssueWatcher(issue.id, authId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: () => {
          this.toastService.success('You are now watching this issue.');
          this.loadIssueDetail(issue.id);
        },
        error: () => this.toastService.error('Failed to update watcher.')
      });
    }
  }

  public updateIssueStatus(issue: Issue, status: string): void {
    this.riskService.patchIssue(issue.id, { status }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.toastService.info(issue.issueCode + ' moved to ' + (this.lookupLabel('ISSUE_STATUS', status) || status) + '.');
          this.loadIssues();
          if (this.selectedIssue()?.id === issue.id) this.loadIssueDetail(issue.id);
          this.changed.emit();
        }
      },
      error: () => this.toastService.error('Failed to update issue status.')
    });
  }

  public deleteIssue(issue: Issue): void {
    if (!confirm(`Delete issue ${issue.issueCode || issue.id}?`)) return;
    this.riskService.deleteIssue(issue.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.toastService.success(`Issue ${issue.issueCode || issue.id} was deleted.`);
          if (this.selectedIssue()?.id === issue.id) {
            this.selectedIssue.set(null);
            this.issueComments.set([]);
            this.issueHistory.set([]);
            this.issueWatchers.set([]);
          }
          this.loadIssues();
          this.changed.emit();
        } else {
          this.toastService.error(res.message || 'Failed to delete the issue.');
        }
      },
      error: () => this.toastService.error('Issue deletion failed.')
    });
  }

  public openCreateIssueModal(): void {
    this.newIssue = {
      title: '',
      description: '',
      severity: 'MEDIUM',
      status: 'OPEN',
      projectId: this.projectId,
      classification: '',
      category: '',
      priority: 'MEDIUM',
      reporterId: this.authService.currentUser()?.id ?? null,
      assigneeId: null,
      milestoneId: null,
      stepsToReproduce: '',
      expectedBehavior: '',
      actualBehavior: '',
      acceptanceCriteria: ''
    };
    this.showCreateIssueModal.set(true);
  }

  public closeCreateIssueModal(): void {
    this.showCreateIssueModal.set(false);
  }

  public submitCreateIssue(): void {
    const payload = {
      projectId: this.projectId,
      title: this.newIssue.title || null,
      description: this.newIssue.description,
      severity: this.newIssue.severity,
      status: this.newIssue.status,
      classification: this.newIssue.classification || null,
      category: this.newIssue.category || null,
      priority: this.newIssue.priority || null,
      reporterId: this.newIssue.reporterId ?? null,
      assigneeId: this.newIssue.assigneeId ?? null,
      milestoneId: this.newIssue.milestoneId ?? null,
      stepsToReproduce: this.newIssue.stepsToReproduce || null,
      expectedBehavior: this.newIssue.expectedBehavior || null,
      actualBehavior: this.newIssue.actualBehavior || null,
      acceptanceCriteria: this.newIssue.acceptanceCriteria || null,
      linkedTaskIds: this.taskCode ? [this.taskCode] : null
    };
    this.submitting.set(true);
    this.riskService.createIssue(payload).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.toastService.success(
            `Issue ${res.data?.issueCode || ''} "${res.data?.title || this.newIssue.title}" was submitted successfully.`
          );
          this.closeCreateIssueModal();
          this.loadIssues();
          this.changed.emit();
        } else {
          this.toastService.error(res.message || 'Failed to submit the issue.');
        }
        this.submitting.set(false);
      },
      error: () => {
        this.toastService.error('Issue submission failed. Please try again.');
        this.submitting.set(false);
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
      category: issue.category || '',
      priority: issue.priority || 'MEDIUM',
      stepsToReproduce: issue.stepsToReproduce || '',
      expectedBehavior: issue.expectedBehavior || '',
      actualBehavior: issue.actualBehavior || '',
      acceptanceCriteria: issue.acceptanceCriteria || '',
      crValue: issue.crValue ?? null,
      crManDays: issue.crManDays ?? null,
      estimatedFixHours: issue.estimatedFixHours ?? null,
      percentage: issue.percentage ?? null,
      reporterId: issue.reporterId ?? issue.ownerId ?? null,
      assigneeId: issue.assigneeId ?? null,
      milestoneId: issue.milestoneId ?? null,
      dueDate: issue.dueDate || ''
    };
    this.showUpdateIssueModal.set(true);
  }

  public closeUpdateIssueModal(): void {
    this.showUpdateIssueModal.set(false);
    this.editingIssueId.set(null);
  }

  public submitUpdateIssue(): void {
    const issueId = this.editingIssueId();
    if (!issueId || this.submitting()) return;
    this.submitting.set(true);
    const oldIssue = this.selectedIssue();
    this.riskService.patchIssue(issueId, this.issueEditForm).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.toastService.success(`Issue ${oldIssue?.issueCode || ''} "${res.data?.title || this.issueEditForm.title}" was updated.`);
          this.closeUpdateIssueModal();
          this.loadIssues();
          if (this.selectedIssue()?.id === issueId) {
            this.selectedIssue.set(res.data || this.selectedIssue());
            this.loadIssueDetail(issueId);
          }
          this.changed.emit();
        } else {
          this.toastService.error(res.message || 'Failed to update the issue.');
        }
        this.submitting.set(false);
      },
      error: () => {
        this.toastService.error('Issue update failed. Please try again.');
        this.submitting.set(false);
      }
    });
  }

  // ── Presentation helpers ──
  private avatarPalette = ['#6366f1', '#0ea5e9', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#14b8a6', '#ec4899'];

  public avatarColor(name?: string): string {
    if (!name) return this.avatarPalette[0];
    let hash = 0;
    for (let i = 0; i < name.length; i++) hash = (hash * 31 + name.charCodeAt(i)) % 997;
    return this.avatarPalette[hash % this.avatarPalette.length];
  }

  public initials(name?: string | null): string {
    if (!name) return '?';
    return name.trim().split(/\s+/).map((p) => p.charAt(0)).join('').slice(0, 2).toUpperCase();
  }

  public statusGradient(status?: string | null): string {
    switch (status) {
      case 'ACHIEVED': case 'COMPLETED': case 'CLOSED': case 'RESOLVED': return 'gr-emerald';
      case 'IN_PROGRESS': return 'gr-sky';
      case 'DELAYED': case 'MISSED': return 'gr-amber';
      case 'CANCELLED': case 'REJECTED': case 'REOPENED': case 'BLOCKED': return 'gr-rose';
      case 'PLANNED': case 'OPEN': case 'TODO': return 'gr-indigo';
      default: return 'gr-slate';
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

  public historyActionLabel(h: IssueHistory): string {
    switch (h.actionType) {
      case 'CREATED': return 'Created issue';
      case 'STATUS_CHANGED': return `Changed status to ${h.newValue || ''}`;
      case 'COMMENT_ADDED': return 'Added a comment';
      case 'WATCHER_ADDED': return 'Added watcher';
      case 'UPDATED': return `Updated ${h.fieldName || 'issue'}`;
      default: return h.actionType || 'Updated';
    }
  }

  public historyActionIcon(actionType?: string): string {
    switch (actionType) {
      case 'CREATED': return 'add_circle';
      case 'STATUS_CHANGED': return 'swap_horiz';
      case 'COMMENT_ADDED': return 'chat_bubble';
      case 'WATCHER_ADDED': return 'notifications_active';
      default: return 'schedule';
    }
  }
}