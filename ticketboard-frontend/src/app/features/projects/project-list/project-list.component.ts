import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ProjectService } from '../../../core/services/project.service';
import { AuthService } from '../../../core/services/auth.service';
import { Project } from '../../../core/models/api.models';

@Component({
  selector: 'app-project-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, MatIconModule, MatTooltipModule],
  templateUrl: './project-list.component.html',
  styleUrls: ['./project-list.component.scss']
})
export class ProjectListComponent implements OnInit {
  public projects = signal<Project[]>([]);
  public filteredProjects = signal<Project[]>([]);
  public isLoading = signal<boolean>(true);
  public activeTab = signal<'active' | 'public'>('active');
  public filterScope = signal<string>('ALL');
  public searchQuery = signal<string>('');

  // Create Project Modal State
  public showCreateModal = signal<boolean>(false);
  public newProject = {
    projectCode: '',
    name: '',
    description: '',
    clientId: 1,
    priority: 'HIGH',
    budget: 50000,
    estimatedHours: 400,
    startDate: new Date().toISOString().substring(0, 10),
    plannedEndDate: ''
  };

  constructor(private projectService: ProjectService, private authService: AuthService) {}

  ngOnInit(): void {
    this.loadProjects();
  }

  public loadProjects(): void {
    this.isLoading.set(true);
    this.projectService.getAllProjects().subscribe({
      next: (res: any) => {
        this.isLoading.set(false);
        if (res.success && res.data) {
          this.projects.set(res.data);
          this.applyFilters();
        }
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  public setTab(tab: 'active' | 'public'): void {
    this.activeTab.set(tab);
    this.applyFilters();
  }

  public applyFilters(): void {
    let list = this.projects();
    const currentUserId = this.authService.currentUser()?.id;

    // Tab split: Active = owned by me; Public = owned by others
    if (this.activeTab() === 'active') {
      list = list.filter((p) => p.projectManagerId === currentUserId);
    } else {
      list = list.filter((p) => p.projectManagerId !== currentUserId);
    }

    if (this.searchQuery().trim()) {
      const q = this.searchQuery().toLowerCase();
      list = list.filter((p) => p.name.toLowerCase().includes(q) || p.projectCode.toLowerCase().includes(q));
    }
    if (this.filterScope() !== 'ALL') {
      list = list.filter((p) => p.status === this.filterScope());
    }
    this.filteredProjects.set(list);
  }

  public isOwnedByMe(p: Project): boolean {
    return p.projectManagerId === this.authService.currentUser()?.id;
  }

  public statusLabel(status: string): string {
    return status.replace(/_/g, ' ');
  }

  public statusClass(status: string): string {
    switch (status) {
      case 'COMPLETED':
      case 'CLOSED':
        return 'green-pill';
      case 'ON_HOLD':
      case 'CANCELLED':
        return 'amber-pill';
      case 'IN_PROGRESS':
      case 'APPROVED':
        return 'blue-pill';
      default:
        return 'slate-pill';
    }
  }

  public taskCounts(p: Project): { closed: number; total: number } {
    const total = p.taskCount || 0;
    const closed = Math.round(total * ((p.completionPercentage || 0) / 100));
    return { closed, total };
  }

  public issueCounts(p: Project): { open: number; total: number } {
    const open = p.openBugCount || 0;
    return { open, total: open };
  }

  public openCreateModal(): void {
    this.showCreateModal.set(true);
  }

  public closeCreateModal(): void {
    this.showCreateModal.set(false);
  }

  public submitCreateProject(): void {
    this.projectService.createProject(this.newProject).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.closeCreateModal();
          this.loadProjects();
        }
      }
    });
  }
}
