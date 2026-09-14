import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RequirementService } from '../../../core/services/requirement.service';
import { ProjectService } from '../../../core/services/project.service';
import { Project, Requirement, RequirementHistory } from '../../../core/models/api.models';

@Component({
  selector: 'app-requirement-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, MatIconModule, MatTooltipModule],
  templateUrl: './requirement-list.component.html',
  styleUrls: ['./requirement-list.component.scss']
})
export class RequirementListComponent implements OnInit {
  public requirements = signal<Requirement[]>([]);
  public projects = signal<Project[]>([]);
  public isLoading = signal<boolean>(true);
  public selectedProjectId = signal<number | null>(null);

  // History Comparison Modal
  public showHistoryModal = signal<boolean>(false);
  public activeRequirement = signal<Requirement | null>(null);
  public historyList = signal<RequirementHistory[]>([]);

  // Create Requirement Modal
  public showCreateModal = signal<boolean>(false);
  public newReq = {
    reqNumber: '',
    title: '',
    description: '',
    businessObjective: '',
    acceptanceCriteria: '',
    priority: 'HIGH',
    projectId: null,
    estimatedEffortHours: 40.0,
    deliveryVersion: 'REL-1.0'
  };

  constructor(
    private requirementService: RequirementService,
    private projectService: ProjectService
  ) {}

  ngOnInit(): void {
    this.projectService.getAllProjects().subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.projects.set(res.data);
          if (this.newReq.projectId == null && res.data.length > 0) {
            this.newReq.projectId = res.data[0].id;
          }
        }
      }
    });
    this.loadRequirements();
  }

  public loadRequirements(): void {
    this.isLoading.set(true);
    const params = this.selectedProjectId() ? { projectId: this.selectedProjectId()! } : undefined;
    this.requirementService.getAllRequirements(params).subscribe({
      next: (res: any) => {
        this.isLoading.set(false);
        if (res.success && res.data) {
          this.requirements.set(res.data);
        }
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  public onFilterProject(): void {
    this.loadRequirements();
  }

  public viewHistory(req: Requirement): void {
    this.activeRequirement.set(req);
    this.requirementService.getHistory(req.id).subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.historyList.set(res.data);
          this.showHistoryModal.set(true);
        }
      }
    });
  }

  public closeHistoryModal(): void {
    this.showHistoryModal.set(false);
  }

  public openCreateModal(): void {
    this.showCreateModal.set(true);
  }

  public closeCreateModal(): void {
    this.showCreateModal.set(false);
  }

  public submitCreateReq(): void {
    this.requirementService.createRequirement(this.newReq).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.closeCreateModal();
          this.loadRequirements();
        }
      }
    });
  }
}
