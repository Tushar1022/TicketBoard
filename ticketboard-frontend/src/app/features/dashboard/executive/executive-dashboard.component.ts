import { Component, OnInit, computed, signal, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DashboardService } from '../../../core/services/dashboard.service';
import { UserService } from '../../../core/services/user.service';
import { ProjectService } from '../../../core/services/project.service';
import { CapacityService } from '../../../core/services/capacity.service';
import { WorkItemService } from '../../../core/services/work-item.service';
import { statusLabel } from '../../../core/config/status.config';
import { EmployeeWorkload, ExecutiveDashboard, Project, Team, User, WorkItem } from '../../../core/models/api.models';

@Component({
  selector: 'app-executive-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, MatIconModule, MatTooltipModule],
  templateUrl: './executive-dashboard.component.html',
  styleUrls: ['./executive-dashboard.component.scss']
})
export class ExecutiveDashboardComponent implements OnInit {
  private destroyRef = inject(DestroyRef);
  public data = signal<ExecutiveDashboard | null>(null);
  public teams = signal<Team[]>([]);
  public projectManagers = signal<User[]>([]);
  public employees = signal<User[]>([]);
  public resources = signal<EmployeeWorkload[]>([]);
  public projects = signal<Project[]>([]);
  public tasks = signal<WorkItem[]>([]);

  public isLoading = signal<boolean>(true);
  public errorMessage = signal<string>('');
  private pendingRequests = 0;

  constructor(
    private dashboardService: DashboardService,
    private userService: UserService,
    private projectService: ProjectService,
    private capacityService: CapacityService,
    private workItemService: WorkItemService
  ) {}

  ngOnInit(): void {
    this.loadAllData();
  }

  public loadAllData(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.pendingRequests = 0;

    this.beginLoad();
    this.dashboardService.getExecutiveDashboard().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success && res.data) this.data.set(res.data);
        this.endLoad();
      },
      error: (err: any) => {
        this.errorMessage.set(err?.error?.message || 'Failed to load executive metrics.');
        this.endLoad();
      }
    });

    this.beginLoad();
    this.userService.getTeams().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success && res.data) this.teams.set(res.data);
        this.endLoad();
      },
      error: () => this.endLoad()
    });

    this.beginLoad();
    this.userService.getProjectOwners().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success && res.data) this.projectManagers.set(res.data);
        this.endLoad();
      },
      error: () => this.endLoad()
    });

    this.beginLoad();
    this.userService.getUsers().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success && res.data) this.employees.set(res.data);
        this.endLoad();
      },
      error: () => this.endLoad()
    });

    this.beginLoad();
    this.capacityService.getEmployeeWorkloads().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success && res.data) this.resources.set(res.data);
        this.endLoad();
      },
      error: () => this.endLoad()
    });

    this.beginLoad();
    this.projectService.getAllProjects().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success && res.data) this.projects.set(res.data);
        this.endLoad();
      },
      error: (err: any) => {
        this.errorMessage.set(err?.error?.message || 'Failed to load projects.');
        this.endLoad();
      }
    });

    this.beginLoad();
    this.workItemService.getWorkItems().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.success && res.data) this.tasks.set(res.data);
        this.endLoad();
      },
      error: () => this.endLoad()
    });
  }

  private beginLoad(): void {
    this.pendingRequests++;
  }

  private endLoad(): void {
    this.pendingRequests--;
    if (this.pendingRequests <= 0) this.isLoading.set(false);
  }

  // ─── Task Status Donut (org-wide work item distribution) ─────────────
  public donutSegments = computed(() => {
    const counts = new Map<string, number>();
    for (const t of this.tasks()) {
      const label = statusLabel(t.status);
      counts.set(label, (counts.get(label) || 0) + 1);
    }
    const items = Array.from(counts.entries())
      .map(([label, count]) => ({ label, count }))
      .sort((a, b) => b.count - a.count);

    const total = items.reduce((acc, it) => acc + it.count, 0) || 1;
    const colors = ['#6366f1', '#3b82f6', '#f59e0b', '#22c55e', '#ef4444', '#8b5cf6', '#14b8a6', '#64748b', '#ec4899', '#f97316'];
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

  public donutTotal = computed(() => this.tasks().length);

  // ─── Org Snapshot Counters ────────────────────────────────────────────
  public activeProjectsCount = computed(() =>
    this.projects().filter((p) => p.status === 'IN_PROGRESS' || p.status === 'APPROVED').length
  );

  public overloadedResourcesCount = computed(() =>
    this.resources().filter((r) => r.isOverloaded || r.utilizationPercentage > 100).length
  );

  public lastUpdatedTime = signal<string>(new Date().toLocaleTimeString());

  public totalOrgCapacity = computed(() => {
    return this.resources().reduce((acc, r) => acc + (r.monthlyCapacityHours || 160), 0);
  });

  public totalAllocatedDemand = computed(() => {
    return this.resources().reduce((acc, r) => acc + (r.allocatedHours || 0), 0);
  });

  public overallOrgUtilization = computed(() => {
    const cap = this.totalOrgCapacity();
    const dem = this.totalAllocatedDemand();
    return cap > 0 ? Math.round((dem / cap) * 100) : 85;
  });

  public pmProjectCount(pmId: number | undefined): number {
    return this.projects().filter((p) => p.projectManagerId === pmId).length;
  }

  public sortResources(): EmployeeWorkload[] {
    return [...this.resources()].sort((a, b) => b.utilizationPercentage - a.utilizationPercentage).slice(0, 14);
  }

  public topEmployees(): User[] {
    return [...this.employees()].slice(0, 14);
  }

  public initials(name?: string): string {
    if (!name) return '?';
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].charAt(0).toUpperCase();
    return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
  }

  public utilBarWidth(util: number): number {
    return Math.max(2, Math.min(100, util));
  }

  public formatStatus(val?: string): string {
    if (!val) return '';
    return val.replace(/_/g, ' ');
  }
}