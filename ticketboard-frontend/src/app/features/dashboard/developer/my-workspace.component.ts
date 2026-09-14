import { Component, OnInit, signal, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { DashboardService } from '../../../core/services/dashboard.service';
import { DeveloperDashboard } from '../../../core/models/api.models';
import { LogTimeDialogComponent } from '../../../shared/components/log-time-dialog/log-time-dialog.component';

@Component({
  selector: 'app-my-workspace',
  standalone: true,
  imports: [CommonModule, RouterModule, MatIconModule, MatTooltipModule, MatDialogModule],
  templateUrl: './my-workspace.component.html',
  styleUrls: ['./my-workspace.component.scss']
})
export class MyWorkspaceComponent implements OnInit {
  private destroyRef = inject(DestroyRef);
  public data = signal<DeveloperDashboard | null>(null);
  public isLoading = signal<boolean>(true);

  constructor(
    private dashboardService: DashboardService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadWorkspaceData();
  }

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

  public openLogTimeModal(): void {
    const ref = this.dialog.open(LogTimeDialogComponent, { width: '560px' });
    ref.afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((saved: boolean) => {
      if (saved) {
        this.loadWorkspaceData();
      }
    });
  }
}
