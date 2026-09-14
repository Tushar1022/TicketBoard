import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ReleaseService } from '../../../core/services/release.service';
import { ProjectService } from '../../../core/services/project.service';
import { Project, Release, ReleaseEnvironment, ReleaseStatus } from '../../../core/models/api.models';

@Component({
  selector: 'app-release-list',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatTooltipModule],
  templateUrl: './release-list.component.html',
  styleUrls: ['./release-list.component.scss']
})
export class ReleaseListComponent implements OnInit {
  public releases = signal<Release[]>([]);
  public projects = signal<Project[]>([]);
  public isLoading = signal<boolean>(true);
  public selectedProjectId = signal<number | null>(null);

  // Environments in pipeline order
  public pipelineEnvironments: ReleaseEnvironment[] = [
    'DEV',
    'SIT',
    'UAT',
    'PRE_PROD',
    'PRODUCTION'
  ];

  // Create Release Modal
  public showCreateModal = signal<boolean>(false);
  public newRelease = {
    releaseVersion: '',
    title: '',
    description: '',
    projectId: null,
    environment: 'UAT' as ReleaseEnvironment,
    plannedDate: new Date(Date.now() + 86400000 * 7).toISOString().substring(0, 10),
    status: 'PLANNED' as ReleaseStatus
  };

  constructor(
    private releaseService: ReleaseService,
    private projectService: ProjectService
  ) {}

  ngOnInit(): void {
    this.projectService.getAllProjects().subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.projects.set(res.data);
          if (this.newRelease.projectId == null && res.data.length > 0) {
            this.newRelease.projectId = res.data[0].id;
          }
        }
      }
    });
    this.loadReleases();
  }

  public loadReleases(): void {
    this.isLoading.set(true);
    this.releaseService.getReleases(this.selectedProjectId() || undefined).subscribe({
      next: (res: any) => {
        this.isLoading.set(false);
        if (res.success && res.data) {
          this.releases.set(res.data);
        }
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  public getPipelineCount(env: ReleaseEnvironment): number {
    return this.releases().filter((r) => r.environment === env).length;
  }

  public openCreateModal(): void {
    this.showCreateModal.set(true);
  }

  public closeCreateModal(): void {
    this.showCreateModal.set(false);
  }

  public submitCreateRelease(): void {
    this.releaseService.createRelease(this.newRelease).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.closeCreateModal();
          this.loadReleases();
        }
      }
    });
  }
}
