import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RiskService } from '../../core/services/risk.service';
import { ProjectService } from '../../core/services/project.service';
import { Issue, Project, Risk, RiskStatus } from '../../core/models/api.models';

@Component({
  selector: 'app-risk-management',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatTooltipModule],
  templateUrl: './risk-management.component.html',
  styleUrls: ['./risk-management.component.scss']
})
export class RiskManagementComponent implements OnInit {
  public risks = signal<Risk[]>([]);
  public issues = signal<Issue[]>([]);
  public projects = signal<Project[]>([]);
  public isLoading = signal<boolean>(true);
  public selectedProjectId = signal<number | null>(null);

  // Create Risk Modal State
  public showCreateRiskModal = signal<boolean>(false);
  public newRisk = {
    riskCode: '',
    projectId: 1,
    description: '',
    probability: 3,
    impact: 4,
    mitigationPlan: '',
    targetDate: new Date(Date.now() + 86400000 * 14).toISOString().substring(0, 10),
    status: 'IDENTIFIED' as RiskStatus
  };

  constructor(
    private riskService: RiskService,
    private projectService: ProjectService
  ) {}

  ngOnInit(): void {
    this.projectService.getAllProjects().subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.projects.set(res.data);
          if (res.data.length > 0) {
            this.selectedProjectId.set(res.data[0].id);
            this.loadRisksAndIssues();
          }
        }
      }
    });
  }

  public loadRisksAndIssues(): void {
    this.isLoading.set(true);
    const pid = this.selectedProjectId() || undefined;

    this.riskService.getRisks(pid).subscribe({
      next: (res: any) => {
        this.isLoading.set(false);
        if (res.success && res.data) {
          this.risks.set(res.data);
        }
      },
      error: () => {
        this.isLoading.set(false);
      }
    });

    if (this.selectedProjectId()) {
      this.riskService.getIssuesByProject(this.selectedProjectId()!).subscribe({
        next: (res: any) => {
          if (res.success && res.data) {
            this.issues.set(res.data);
          }
        }
      });
    }
  }

  public getRisksByCell(prob: number, imp: number): Risk[] {
    return this.risks().filter((r) => r.probability === prob && r.impact === imp);
  }

  public openCreateRiskModal(): void {
    this.showCreateRiskModal.set(true);
  }

  public closeCreateRiskModal(): void {
    this.showCreateRiskModal.set(false);
  }

  public submitCreateRisk(): void {
    this.riskService.createRisk(this.newRisk).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.closeCreateRiskModal();
          this.loadRisksAndIssues();
        }
      }
    });
  }
}
