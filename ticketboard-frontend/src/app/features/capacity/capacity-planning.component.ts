import { Component, OnInit, signal, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CapacityService } from '../../core/services/capacity.service';
import { EmployeeWorkload, ProjectForecast, RequirementForecast, TeamCapacity } from '../../core/models/api.models';

@Component({
  selector: 'app-capacity-planning',
  standalone: true,
  imports: [CommonModule, RouterModule, MatIconModule, MatTooltipModule],
  templateUrl: './capacity-planning.component.html',
  styleUrls: ['./capacity-planning.component.scss']
})
export class CapacityPlanningComponent implements OnInit {
  public workloads = signal<EmployeeWorkload[]>([]);
  public teams = signal<TeamCapacity[]>([]);
  public projections = signal<ProjectForecast[]>([]);
  public demandForecasts = signal<RequirementForecast[]>([]);
  public isLoading = signal<boolean>(true);

  constructor(private capacityService: CapacityService) {}

  private destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.loadAllCapacityMetrics();
  }

  public loadAllCapacityMetrics(): void {
    this.isLoading.set(true);

    forkJoin({
      workloads: this.capacityService.getEmployeeWorkloads(),
      teams: this.capacityService.getTeamCapacities(),
      projections: this.capacityService.getProjectProjections(),
      forecasts: this.capacityService.getDemandForecast()
    })
    .pipe(takeUntilDestroyed(this.destroyRef))
    .subscribe({
      next: (res: any) => {
        if (res.workloads.success && res.workloads.data) this.workloads.set(res.workloads.data);
        if (res.teams.success && res.teams.data) this.teams.set(res.teams.data);
        if (res.projections.success && res.projections.data) this.projections.set(res.projections.data);
        if (res.forecasts.success && res.forecasts.data) this.demandForecasts.set(res.forecasts.data);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }
}
