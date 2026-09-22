import { Component, OnInit, OnDestroy, signal, computed, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { HttpClient } from '@angular/common/http';
import { DashboardService } from '../../../core/services/dashboard.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { DeveloperDashboard, DeveloperTelemetry, ERPAsset, ExpenseClaim, LeaveRequestApi, OKRGoalApi, FocusSession, ActivityLog, WorkItem } from '../../../core/models/api.models';
import { FocusSessionService } from '../../../core/services/focus-session.service';
import { WorkspaceRealtimeService } from '../../../core/services/workspace-realtime.service';
import { ErpService } from '../../../core/services/erp.service';
import { LogTimeDialogComponent } from '../../../shared/components/log-time-dialog/log-time-dialog.component';
import { ThemePickerComponent } from '../../../shared/components/theme-picker/theme-picker.component';

@Component({
  selector: 'app-my-workspace',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatIconModule,
    MatTooltipModule,
    MatDialogModule,
    ThemePickerComponent
  ],
  templateUrl: './my-workspace.component.html',
  styleUrls: ['./my-workspace.component.scss']
})
export class MyWorkspaceComponent implements OnInit, OnDestroy {
  private destroyRef = inject(DestroyRef);
  private readonly http = inject(HttpClient);
  public readonly authService = inject(AuthService);
  private readonly toastService = inject(ToastService);
  private readonly dashboardService = inject(DashboardService);
  private readonly focusSessionService = inject(FocusSessionService);
  private readonly realtimeService = inject(WorkspaceRealtimeService);
  private readonly erpService = inject(ErpService);
  private readonly dialog = inject(MatDialog);

  public data = signal<DeveloperDashboard | null>(null);
  public telemetry = signal<DeveloperTelemetry | null>(null);
  public isLoading = signal<boolean>(true);
  public activeTab = signal<'cockpit' | 'erp' | 'tools' | 'profile' | 'themes'>('cockpit');

  // ─── Live DB Telemetry & WebSocket ───────────────────────────────────────
  public wsConnected = computed(() => this.realtimeService.connected());
  private readonly httpGet = (url: string) => this.http.get<any>(url);

  public get dbBadgeLabel(): string {
    const t = this.telemetry();
    if (!t) return 'DB — unavailable';
    const product = t.dbProduct || '—';
    const version = t.dbVersion || '—';
    const latency = typeof t.latencyMs === 'number' && t.latencyMs > 0 ? `${t.latencyMs.toFixed(2)}ms` : '—';
    return `${product} ${version} | ${latency}`;
  }

  // ─── Live Focus Stopwatch (backend-persisted) ────────────────────────────
  public timerSeconds = signal<number>(0);
  public isTimerRunning = signal<boolean>(false);
  private timerInterval: any = null;
  public selectedWorkItemId = signal<number | null>(null);
  public focusSession = signal<FocusSession | null>(null);

  public stopwatchTaskOptions = computed<WorkItem[]>(() => {
    const d = this.data();
    if (!d) return [];
    const map = new Map<number, WorkItem>();
    for (const w of d.myActiveTasks ?? []) map.set(w.id, w);
    for (const w of d.upcomingDeadlines ?? []) map.set(w.id, w);
    return Array.from(map.values());
  });

  public get selectedTaskLabel(): string {
    const id = this.selectedWorkItemId();
    const option = this.stopwatchTaskOptions().find(w => w.id === id);
    return option ? `${option.ticketNumber}: ${option.title}` : 'Select a task';
  }

  public weeklyLoggedPercent(): number {
    const capacity = this.data()?.weeklyCapacityHours ?? 0;
    const logged = this.data()?.hoursLoggedThisWeek ?? 0;
    if (capacity <= 0) return 0;
    return Math.min(100, Math.round((logged / capacity) * 100));
  }

  public profileInitials(): string {
    const u = this.authService.currentUser();
    if (u?.firstName && u?.lastName) {
      return `${u.firstName[0]}${u.lastName[0]}`.toUpperCase();
    }
    if (u?.firstName) return u.firstName[0].toUpperCase();
    return '—';
  }

  // ─── Developer Tools State ───────────────────────────────────────────────
  public scratchpadText = signal<string>(localStorage.getItem('tb_workspace_scratchpad') || '');

  public apiMethod = signal<'GET' | 'POST' | 'PUT' | 'DELETE'>('GET');
  public apiUrl = signal<string>('/api/v1/time-entries/my');
  public apiPayload = signal<string>('{\n  "projectId": 1,\n  "totalHours": 1.0,\n  "description": "Logged from workspace API tester"\n}');
  public apiResponse = signal<string>('Execute an API test to view the real live response.');
  public apiLatency = signal<number | null>(null);

  public jwtInput = signal<string>('');
  public jwtDecoded = signal<string>('');

  public base64Input = signal<string>('');
  public base64Output = signal<string>('');
  public epochInput = signal<number>(Date.now());
  public epochFormatted = signal<string>('');

  public standupYesterday = signal<string>('');
  public standupToday = signal<string>('');
  public standupBlockers = signal<string>('');
  public generatedStandup = signal<string>('');

  // ─── ERP Data Lists (real backend data only) ────────────────────────────
  public assets = signal<ERPAsset[]>([]);
  public expenseClaims = signal<ExpenseClaim[]>([]);
  public leaveRequests = signal<LeaveRequestApi[]>([]);
  public okrGoals = signal<OKRGoalApi[]>([]);
  public recentActivities = signal<ActivityLog[]>([]);

  public leaveTotals = computed(() => {
    const list = this.leaveRequests();
    const approved = list.filter(l => l.status === 'APPROVED').reduce((s, l) => s + (l.totalDays ?? 0), 0);
    const pending = list.filter(l => l.status === 'PENDING').reduce((s, l) => s + (l.totalDays ?? 0), 0);
    return { approved, pending, total: approved + pending };
  });

  // Modals visibility
  public showExpenseModal = signal<boolean>(false);
  public showLeaveModal = signal<boolean>(false);
  public showAssetModal = signal<boolean>(false);

  // New item form inputs
  public newExpense = { category: 'Software & Tools', amount: 150, currency: 'USD', description: '' };
  public newLeave = { type: 'Annual Paid', startDate: '', endDate: '', reason: '' };
  public newAsset = { name: '', category: 'Hardware' };

  constructor() {}

  ngOnInit(): void {
    this.loadWorkspaceData();
    this.refreshTelemetry();
    this.fetchBackendERPData();
    this.loadFocusSession();
    this.fetchRecentActivities();
    this.primeStopwatchTask();
    this.primeStandup();
    this.connectRealtime();
    this.decodeJwtToken();
    this.encodeBase64();
    this.convertEpoch();

    const telemetryTimer = setInterval(() => this.refreshTelemetry(), 30000);
    this.destroyRef.onDestroy(() => clearInterval(telemetryTimer));
  }

  ngOnDestroy(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
    this.realtimeService.disconnect();
  }

  private connectRealtime(): void {
    this.realtimeService.connect();
    this.realtimeService.messages$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((msg) => {
        if (!msg) return;
        if (msg.type === 'FOCUS_LOGGED') {
          this.loadWorkspaceData();
          this.loadFocusSession();
          return;
        }
        this.loadWorkspaceData();
        this.loadFocusSession();
        this.refreshTelemetry();
      });
  }

  // ─── Data Loading ────────────────────────────────────────────────────────
  public loadWorkspaceData(): void {
    this.isLoading.set(true);
    this.dashboardService.getDeveloperDashboard().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        this.isLoading.set(false);
        if (res.success && res.data) {
          this.data.set(res.data);
        }
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  public refreshTelemetry(): void {
    this.dashboardService.getDeveloperTelemetry().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.telemetry.set(res.data);
        }
      },
      error: () => {}
    });
  }

  private loadFocusSession(): void {
    this.focusSessionService.getCurrent().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          const s: FocusSession = res.data;
          this.focusSession.set(s);
          this.timerSeconds.set(s.elapsedSeconds ?? 0);
          this.isTimerRunning.set(s.status === 'RUNNING');
          if (s.workItemId) {
            this.selectedWorkItemId.set(s.workItemId);
          }
          if (s.status === 'RUNNING' && !this.timerInterval) {
            this.startTicker();
          }
        }
      },
      error: () => {}
    });
  }

  private fetchBackendERPData(): void {
    this.erpService.getAssets().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res) => this.assets.set(res.success && res.data ? res.data : []),
      error: () => {}
    });

    this.erpService.getExpenseClaims().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res) => this.expenseClaims.set(res.success && res.data ? res.data : []),
      error: () => {}
    });

    this.erpService.getLeaveRequests().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res) => this.leaveRequests.set(res.success && res.data ? res.data : []),
      error: () => {}
    });

    this.erpService.getOkrGoals().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res) => this.okrGoals.set(res.success && res.data ? res.data : []),
      error: () => {}
    });
  }

  private fetchRecentActivities(): void {
    this.httpGet('http://localhost:8080/api/v1/activity-logs/recent')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res: any) => {
          if (res.success && res.data) {
            const userId = this.authService.currentUser()?.id;
            const mine = userId
              ? res.data.filter((a: ActivityLog) => a.userId === userId)
              : res.data;
            this.recentActivities.set(mine.slice(0, 12));
          }
        },
        error: () => {}
      });
  }

  private primeStopwatchTask(): void {
    // Once dashboard data is available, select the first real active task if none chosen
    const check = () => {
      const options = this.stopwatchTaskOptions();
      if (options.length > 0) {
        let current = this.selectedWorkItemId();
        if (current == null || !options.some(o => o.id === current)) {
          current = options[0].id;
          this.selectedWorkItemId.set(current);
        }
      }
    };
    check();
    setTimeout(check, 500);
    setTimeout(check, 1500);
  }

  private primeStandup(): void {
    const check = () => {
      const d = this.data();
      if (d) {
        if (!this.standupYesterday()) {
          const yesterdayEntries = (d.recentTimeEntries ?? []).slice(0, 3).map(e => e.description || e.workItemTicketNumber || 'Time entry').join('; ');
          this.standupYesterday.set(yesterdayEntries || 'Completed development tasks and delivered sprint items.');
        }
        if (!this.standupToday()) {
          const active = (d.myActiveTasks ?? []).slice(0, 3).map(t => `${t.ticketNumber} ${t.title}`.trim()).join('; ');
          this.standupToday.set(active || 'Focusing on assigned sprint tasks.');
        }
        if (!this.standupBlockers()) {
          const blocked = (d.myBlockedTasks ?? []).map(t => `${t.ticketNumber}: ${t.blockedReason || 'blocked'}`).join('; ');
          this.standupBlockers.set(blocked || 'None.');
        }
      }
    };
    check();
    setTimeout(check, 500);
    setTimeout(check, 1500);
  }

  // ─── Stopwatch Timer Actions (backend-persisted) ─────────────────────────
  public startStopwatch(): void {
    if (this.isTimerRunning()) return;
    const workItemId = this.selectedWorkItemId();
    if (workItemId == null) {
      this.toastService.warning('Select an active task first.');
      return;
    }
    this.focusSessionService.start(workItemId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.focusSession.set(res.data);
          this.timerSeconds.set(res.data.elapsedSeconds ?? 0);
          this.isTimerRunning.set(true);
          this.startTicker();
          this.toastService.info('Live Focus Timer started.');
        }
      },
      error: (err: any) => this.toastService.error(err?.error?.message || 'Could not start focus session.')
    });
  }

  public pauseStopwatch(): void {
    if (!this.isTimerRunning()) return;
    this.focusSessionService.pause().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.focusSession.set(res.data);
          this.timerSeconds.set(res.data.elapsedSeconds ?? this.timerSeconds());
          this.isTimerRunning.set(false);
          this.stopTicker();
          this.toastService.info('Live Focus Timer paused.');
        }
      },
      error: (err: any) => this.toastService.error(err?.error?.message || 'Could not pause focus session.')
    });
  }

  public resetStopwatch(): void {
    this.focusSessionService.reset().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.timerSeconds.set(0);
          this.isTimerRunning.set(false);
          this.stopTicker();
          this.toastService.info('Stopwatch reset to 00:00:00.');
        }
      },
      error: (err: any) => this.toastService.error(err?.error?.message || 'Could not reset focus session.')
    });
  }

  private startTicker(): void {
    if (this.timerInterval) return;
    this.timerInterval = setInterval(() => {
      this.timerSeconds.update(s => s + 1);
    }, 1000);
  }

  private stopTicker(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }

  public formatTimer(totalSeconds: number): string {
    const valid = Number.isFinite(totalSeconds) ? Math.max(0, Math.floor(totalSeconds)) : 0;
    const hrs = Math.floor(valid / 3600);
    const mins = Math.floor((valid % 3600) / 60);
    const secs = valid % 60;
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${pad(hrs)}:${pad(mins)}:${pad(secs)}`;
  }

  public logTimerTime(): void {
    if (this.timerSeconds() < 60) {
      this.toastService.warning('Run the timer for at least 1 minute before logging.');
      return;
    }
    this.focusSessionService.logTime().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success) {
          const hours = (this.timerSeconds() / 3600).toFixed(2);
          this.toastService.success(`Logged ${hours}h focus time to ${this.selectedTaskLabel}`);
          this.timerSeconds.set(0);
          this.isTimerRunning.set(false);
          this.stopTicker();
          this.loadWorkspaceData();
        }
      },
      error: (err: any) => this.toastService.error(err?.error?.message || 'Could not log focus time.')
    });
  }

  // ─── Developer Tools Actions ─────────────────────────────────────────────
  public updateScratchpad(val: string): void {
    this.scratchpadText.set(val);
    localStorage.setItem('tb_workspace_scratchpad', val);
  }

  public executeApiTest(): void {
    const method = this.apiMethod();
    const url = this.apiUrl();
    const start = performance.now();
    this.apiResponse.set('Executing request to ' + url + '...');

    const obs = method === 'GET'
      ? this.http.get<any>(url)
      : method === 'POST'
        ? this.http.post<any>(url, this.safePayload())
        : method === 'PUT'
          ? this.http.put<any>(url, this.safePayload())
          : this.http.delete<any>(url);

    obs.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (body: any) => {
        const duration = Math.round(performance.now() - start);
        this.apiLatency.set(duration);
        this.apiResponse.set(JSON.stringify(body, null, 2));
        this.toastService.success(`API test completed in ${duration}ms`);
      },
      error: (err: any) => {
        const duration = Math.round(performance.now() - start);
        this.apiLatency.set(duration);
        this.apiResponse.set(JSON.stringify({
          status: err?.status,
          statusText: err?.statusText,
          error: err?.error
        }, null, 2));
        this.toastService.error(`API test failed (${duration}ms)`);
      }
    });
  }

  private safePayload(): any {
    try {
      return JSON.parse(this.apiPayload());
    } catch {
      return {};
    }
  }

  public decodeJwtToken(): void {
    const token = this.jwtInput() || (localStorage.getItem('tb_token') || '');
    if (!token) {
      this.jwtDecoded.set('Paste a JWT or use your active session token.');
      return;
    }
    try {
      const parts = token.split('.');
      if (parts.length >= 2) {
        const payloadDecoded = atob(parts[1].replace(/-/g, '+').replace(/_/g, '/'));
        this.jwtDecoded.set(JSON.stringify(JSON.parse(payloadDecoded), null, 2));
      } else {
        this.jwtDecoded.set('Invalid JWT Token structure (Expected header.payload.signature)');
      }
    } catch {
      this.jwtDecoded.set('Error decoding JWT token base64 payload.');
    }
  }

  public encodeBase64(): void {
    try {
      this.base64Output.set(btoa(this.base64Input()));
    } catch {
      this.base64Output.set('Encoding error');
    }
  }

  public convertEpoch(): void {
    try {
      const d = new Date(Number(this.epochInput()));
      this.epochFormatted.set(d.toUTCString() + ' | ' + d.toLocaleString());
    } catch {
      this.epochFormatted.set('Invalid Epoch timestamp');
    }
  }

  public generateStandupNotes(): void {
    const note = `### 🚀 Daily Standup & EOD Report (${new Date().toLocaleDateString()})
**Yesterday:**
- ${this.standupYesterday()}

**Today:**
- ${this.standupToday()}

**Blockers / Risks:**
- ${this.standupBlockers()}
`;
    this.generatedStandup.set(note);
    this.toastService.success('Daily Standup notes generated!');
  }

  public copyToClipboard(text: string, label: string): void {
    navigator.clipboard.writeText(text);
    this.toastService.success(`Copied ${label} to clipboard!`);
  }

  // ─── ERP Form Actions (persist to backend, then reload) ──────────────────
  public submitExpense(): void {
    if (!this.newExpense.description) {
      this.toastService.warning('Please enter a description for the expense claim.');
      return;
    }
    const payload = {
      category: this.newExpense.category,
      amount: this.newExpense.amount,
      currency: this.newExpense.currency,
      description: this.newExpense.description
    };
    this.erpService.createExpenseClaim(payload).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.showExpenseModal.set(false);
          this.newExpense.description = '';
          this.fetchBackendERPData();
          this.toastService.success('Expense claim submitted for manager approval!');
        }
      },
      error: (err: any) => this.toastService.error(err?.error?.message || 'Failed to submit expense claim.')
    });
  }

  public submitLeave(): void {
    if (!this.newLeave.startDate || !this.newLeave.endDate) {
      this.toastService.warning('Please select start and end dates.');
      return;
    }
    const request = {
      leaveType: this.newLeave.type,
      startDate: this.newLeave.startDate,
      endDate: this.newLeave.endDate,
      reason: this.newLeave.reason || 'Personal leave request'
    };
    this.erpService.createLeaveRequest(request).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.showLeaveModal.set(false);
          this.newLeave.startDate = '';
          this.newLeave.endDate = '';
          this.newLeave.reason = '';
          this.fetchBackendERPData();
          this.toastService.success('Leave request submitted!');
        }
      },
      error: (err: any) => this.toastService.error(err?.error?.message || 'Failed to submit leave request.')
    });
  }

  public submitAssetRequest(): void {
    if (!this.newAsset.name) {
      this.toastService.warning('Please specify hardware or software name.');
      return;
    }
    this.erpService.createAssetRequest(this.newAsset).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.showAssetModal.set(false);
          this.newAsset.name = '';
          this.fetchBackendERPData();
          this.toastService.success('Asset request submitted to IT Admin!');
        }
      },
      error: (err: any) => this.toastService.error(err?.error?.message || 'Failed to submit asset request.')
    });
  }

  public openLogTimeModal(): void {
    const ref = this.dialog.open(LogTimeDialogComponent, { width: '560px' });
    ref.afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((saved: boolean) => {
      if (saved) {
        this.loadWorkspaceData();
      }
    });
  }

  public formatActivityTime(iso: string | undefined): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleString(undefined, {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  public activityIcon(entityType: string): string {
    switch ((entityType || '').toUpperCase()) {
      case 'WORK_ITEM': return 'task_alt';
      case 'PROJECT': return 'account_tree';
      case 'REQUIREMENT': return 'list_alt';
      case 'TIMESHEET': return 'schedule';
      case 'RELEASE': return 'rocket_launch';
      default: return 'history';
    }
  }
}