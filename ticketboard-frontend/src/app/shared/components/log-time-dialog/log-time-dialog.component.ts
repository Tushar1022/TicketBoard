import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { ProjectService } from '../../../core/services/project.service';
import { WorkItemService } from '../../../core/services/work-item.service';
import { TimeTrackingService } from '../../../core/services/timetracking.service';
import { Project, WorkItem } from '../../../core/models/api.models';

@Component({
  selector: 'app-log-time-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatIconModule],
  templateUrl: './log-time-dialog.component.html',
  styleUrls: ['./log-time-dialog.component.scss']
})
export class LogTimeDialogComponent implements OnInit {
  public projects = signal<Project[]>([]);
  public workItems = signal<WorkItem[]>([]);
  public isSubmitting = signal<boolean>(false);
  public errorMessage = signal<string>('');

  public selectedProjectId: number | null = null;
  public selectedWorkItemId: number | null = null;
  public workDate: string = new Date().toISOString().substring(0, 10);
  public startTime: string = '09:30';
  public endTime: string = '18:00';
  public breakMinutes: number = 60;
  public calculatedHours: number = 7.5;
  public description: string = '';

  constructor(
    private dialogRef: MatDialogRef<LogTimeDialogComponent>,
    private projectService: ProjectService,
    private workItemService: WorkItemService,
    private timeTrackingService: TimeTrackingService
  ) {}

  ngOnInit(): void {
    this.projectService.getAllProjects().subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.projects.set(res.data);
          if (res.data.length > 0) {
            this.selectedProjectId = res.data[0].id;
            if (this.selectedProjectId != null) {
              this.loadWorkItems(this.selectedProjectId);
            }
          }
        }
      }
    });
    this.recalculateHours();
  }

  public onProjectChange(): void {
    if (this.selectedProjectId != null) {
      this.loadWorkItems(this.selectedProjectId);
    }
  }

  private loadWorkItems(projectId: number): void {
    this.workItemService.getWorkItems({ projectId }).subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.workItems.set(res.data);
          if (res.data.length > 0) {
            this.selectedWorkItemId = res.data[0].id;
          }
        }
      }
    });
  }

  public recalculateHours(): void {
    if (!this.startTime || !this.endTime) return;
    const [startH, startM] = this.startTime.split(':').map(Number);
    const [endH, endM] = this.endTime.split(':').map(Number);

    const startTotal = startH * 60 + startM;
    const endTotal = endH * 60 + endM;

    let diff = endTotal - startTotal - (this.breakMinutes || 0);
    if (diff < 0) diff = 0;

    this.calculatedHours = Math.round((diff / 60.0) * 10) / 10;
  }

  public submit(): void {
    if (!this.selectedProjectId || !this.description) {
      this.errorMessage.set('Please select a project and enter a work description.');
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    const payload = {
      projectId: this.selectedProjectId,
      workItemId: this.selectedWorkItemId || undefined,
      workDate: this.workDate,
      startTime: this.startTime,
      endTime: this.endTime,
      breakMinutes: this.breakMinutes,
      totalHours: this.calculatedHours,
      description: this.description
    };

    this.timeTrackingService.logTime(payload).subscribe({
      next: (res: any) => {
        this.isSubmitting.set(false);
        if (res.success) {
          this.dialogRef.close(true);
        }
      },
      error: (err: any) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(err?.error?.message || 'Failed to log time. Please check your inputs.');
      }
    });
  }

  public close(): void {
    this.dialogRef.close(false);
  }
}
