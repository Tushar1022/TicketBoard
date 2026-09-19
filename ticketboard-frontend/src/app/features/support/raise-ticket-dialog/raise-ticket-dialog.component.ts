import { Component, Inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { SupportTicketService } from '../../../core/services/support-ticket.service';
import { AuthService } from '../../../core/services/auth.service';
import { ProjectService } from '../../../core/services/project.service';
import { SupportCategory, SupportTicket, TicketPriority, Project } from '../../../core/models/api.models';
import { ToastService } from '../../../shared/components/toast/toast.service';

@Component({
  selector: 'app-raise-ticket-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatIconModule],
  templateUrl: './raise-ticket-dialog.component.html',
  styleUrls: ['./raise-ticket-dialog.component.scss']
})
export class RaiseTicketDialogComponent implements OnInit {
  public subject = '';
  public category: SupportCategory = 'SYSTEM_DEFECT';
  public customCategoryName = '';
  public priority: TicketPriority = 'HIGH';
  public targetRole: 'ROLE_SUPER_ADMIN' | 'ROLE_ADMIN' = 'ROLE_SUPER_ADMIN';
  public selectedProjectId: number | null = null;
  public moduleName = '';
  public description = '';
  public includeDiagnostics = true;
  public selectedFiles: File[] = [];

  public projects = signal<Project[]>([]);
  public isSubmitting = signal<boolean>(false);
  public successMessage = signal<string>('');
  public errorMessage = signal<string>('');

  constructor(
    public dialogRef: MatDialogRef<RaiseTicketDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private supportTicketService: SupportTicketService,
    private authService: AuthService,
    private projectService: ProjectService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    if (this.data?.targetRole) {
      this.targetRole = this.data.targetRole;
    }
    this.loadProjects();
  }

  private loadProjects(): void {
    this.projectService.getAllProjects().subscribe({
      next: (res: any) => {
        if (res?.success && Array.isArray(res.data)) {
          this.projects.set(res.data);
        }
      }
    });
  }

  public onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      const files = Array.from(input.files);
      this.selectedFiles = [...this.selectedFiles, ...files];
    }
  }

  public removeFile(index: number): void {
    this.selectedFiles.splice(index, 1);
  }

  public submitTicket(): void {
    if (!this.subject.trim()) {
      this.errorMessage.set('Subject is required.');
      return;
    }
    if (this.category === 'CUSTOM_ISSUE' && !this.customCategoryName.trim()) {
      this.errorMessage.set('Please specify your custom category name.');
      return;
    }
    if (!this.description.trim()) {
      this.errorMessage.set('Description is required.');
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    const user = this.authService.currentUser();
    const selectedProj = this.projects().find(p => p.id === Number(this.selectedProjectId));

    const diagnostics = this.includeDiagnostics
      ? `Tenant: ${user?.email?.split('@')[1]?.split('.')[0] || 'Unknown'} | User: ${user?.fullName} (${user?.email}) | OS: ${navigator.platform} | Time: ${new Date().toLocaleString()}`
      : 'Diagnostics omitted by user.';

    this.supportTicketService.createTicket({
      subject: this.subject.trim(),
      category: this.category,
      customCategoryName: this.category === 'CUSTOM_ISSUE' ? this.customCategoryName.trim() : undefined,
      priority: this.priority,
      targetRole: this.targetRole,
      projectId: selectedProj ? selectedProj.id : undefined,
      projectName: selectedProj ? selectedProj.name : undefined,
      moduleName: this.moduleName.trim() || undefined,
      description: this.description.trim(),
      systemDiagnostics: diagnostics
    }).subscribe({
      next: (ticket: SupportTicket) => {
        if (this.selectedFiles.length > 0) {
          let uploaded = 0;
          this.selectedFiles.forEach(file => {
            this.supportTicketService.uploadAttachment(ticket.id, file).subscribe({
              next: () => {
                uploaded++;
                if (uploaded === this.selectedFiles.length) {
                  this.finishSuccess(ticket);
                }
              },
              error: () => {
                uploaded++;
                if (uploaded === this.selectedFiles.length) {
                  this.finishSuccess(ticket);
                }
              }
            });
          });
        } else {
          this.finishSuccess(ticket);
        }
      },
      error: (err: any) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(err?.message || 'Failed to submit support ticket.');
        this.toastService.error('Failed to submit support ticket.');
      }
    });
  }

  private finishSuccess(ticket: SupportTicket): void {
    this.isSubmitting.set(false);
    this.successMessage.set(`Support ticket ${ticket.ticketCode} raised successfully!`);
    this.toastService.success(`Support ticket ${ticket.ticketCode} was raised successfully.`);
    setTimeout(() => {
      this.dialogRef.close(ticket);
    }, 1200);
  }

  public cancel(): void {
    this.dialogRef.close();
  }
}
