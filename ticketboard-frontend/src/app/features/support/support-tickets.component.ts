import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { SupportTicketService } from '../../core/services/support-ticket.service';
import { AuthService } from '../../core/services/auth.service';
import { SupportTicket, TicketStatus, SupportCategory } from '../../core/models/api.models';
import { RaiseTicketDialogComponent } from './raise-ticket-dialog/raise-ticket-dialog.component';
import { ToastService } from '../../shared/components/toast/toast.service';

type TabKey = 'MY_TICKETS' | 'ADMIN_QUEUE';

@Component({
  selector: 'app-support-tickets',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, MatIconModule, MatDialogModule, MatTooltipModule],
  templateUrl: './support-tickets.component.html',
  styleUrls: ['./support-tickets.component.scss']
})
export class SupportTicketsComponent implements OnInit {
  public activeTab = signal<TabKey>('MY_TICKETS');
  public searchQuery = signal<string>('');
  public selectedStatus = signal<string>('ALL');
  public selectedPriority = signal<string>('ALL');
  public selectedCategory = signal<string>('ALL');

  public selectedTicket = signal<SupportTicket | null>(null);

  public resolutionNotesInput = '';
  public statusUpdateInput: TicketStatus = 'RESOLVED';
  public commentInput = '';

  public currentUser = computed(() => this.authService.currentUser());
  public isAdminUser = computed(() => this.authService.isAdmin());
  public isSuperAdmin = computed(() => (this.currentUser()?.roles || []).includes('ROLE_SUPER_ADMIN'));

  public tickets = this.supportTicketService.tickets;
  public isLoading = computed(() => this.supportTicketService.isLoading());

  public statusOptions: TicketStatus[] = ['OPEN', 'IN_REVIEW', 'RESOLVED', 'CLOSED'];
  public categoryOptions: SupportCategory[] = ['SYSTEM_DEFECT', 'ACCESS_REQUEST', 'DATA_QUERY', 'PERFORMANCE_ISSUE', 'BILLING_SLA', 'CUSTOM_ISSUE', 'OTHER'];

  public today = new Date();

  public filteredTickets = computed<SupportTicket[]>(() => {
    const userEmail = (this.currentUser()?.email || '').toLowerCase();
    const tab = this.activeTab();
    const q = this.searchQuery().toLowerCase().trim();
    const status = this.selectedStatus();
    const priority = this.selectedPriority();
    const category = this.selectedCategory();

    return this.tickets().filter(t => {
      const matchesTab = tab === 'ADMIN_QUEUE' ? true : t.createdByEmail.toLowerCase() === userEmail;
      const matchesStatus = status === 'ALL' || t.status === status;
      const matchesPriority = priority === 'ALL' || t.priority === priority;
      const matchesCategory = category === 'ALL' || t.category === category;
      const matchesSearch =
        !q ||
        t.ticketCode.toLowerCase().includes(q) ||
        t.subject.toLowerCase().includes(q) ||
        t.createdByName.toLowerCase().includes(q) ||
        t.createdByEmail.toLowerCase().includes(q) ||
        t.category.toLowerCase().includes(q);

      return matchesTab && matchesStatus && matchesPriority && matchesCategory && matchesSearch;
    });
  });

  public kpiCards = computed(() => {
    const list = this.filteredTickets();
    return [
      { key: 'total', label: 'Total Tickets', value: list.length, icon: 'confirmation_number', tone: 'indigo' },
      { key: 'open', label: 'Open', value: list.filter(t => t.status === 'OPEN').length, icon: 'schedule', tone: 'amber' },
      { key: 'inreview', label: 'In Review', value: list.filter(t => t.status === 'IN_REVIEW').length, icon: 'manage_search', tone: 'blue' },
      { key: 'resolved', label: 'Resolved', value: list.filter(t => t.status === 'RESOLVED').length, icon: 'verified', tone: 'green' },
      { key: 'urgent', label: 'Urgent', value: list.filter(t => t.priority === 'URGENT').length, icon: 'error', tone: 'red' }
    ];
  });

  constructor(
    public supportTicketService: SupportTicketService,
    public authService: AuthService,
    private dialog: MatDialog,
    private route: ActivatedRoute,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['view'] === 'admin' && this.isAdminUser()) {
        this.activeTab.set('ADMIN_QUEUE');
      }
    });

    this.supportTicketService.refreshAll();

    // Preselect the most recently created ticket after load settles
    const current = this.selectedTicket();
    if (!current && this.tickets().length > 0 && !this.tickets()[0]) {
      // no-op guard for reactive binding below
    }
  }

  public selectTicket(ticket: SupportTicket): void {
    this.selectedTicket.set(ticket);
    this.resolutionNotesInput = ticket.resolutionNotes || '';
    this.statusUpdateInput = ticket.status;
  }

  public resetFilters(): void {
    this.searchQuery.set('');
    this.selectedStatus.set('ALL');
    this.selectedPriority.set('ALL');
    this.selectedCategory.set('ALL');
  }

  public openRaiseTicketDialog(targetRole: 'ROLE_SUPER_ADMIN' | 'ROLE_ADMIN' = 'ROLE_SUPER_ADMIN'): void {
    const dialogRef = this.dialog.open(RaiseTicketDialogComponent, {
      width: '680px',
      maxWidth: '95vw',
      data: { targetRole }
    });

    dialogRef.afterClosed().subscribe(res => {
      if (res) {
        this.supportTicketService.refreshAll();
        this.selectedTicket.set(res);
      }
    });
  }

  public updateTicketStatus(): void {
    const ticket = this.selectedTicket();
    if (!ticket) return;

    const adminName = `${this.currentUser()?.fullName} (${this.isSuperAdmin() ? 'Super Admin' : 'Admin'})`;

    this.supportTicketService.updateTicketStatus(
      ticket.id,
      this.statusUpdateInput,
      this.resolutionNotesInput.trim(),
      adminName
    ).subscribe(updated => {
      if (updated) {
        this.toastService.success(`Ticket ${updated.ticketCode} status updated to ${updated.status.replace(/_/g, ' ')}.`);
        this.supportTicketService.refreshAll();
        const fresh = this.tickets().find(t => t.id === updated.id);
        this.selectedTicket.set(fresh || updated);
      }
    });
  }

  public addComment(): void {
    const ticket = this.selectedTicket();
    if (!ticket || !this.commentInput.trim()) return;

    this.supportTicketService.addComment(ticket.id, this.commentInput.trim()).subscribe(() => {
      this.toastService.success('Comment added to the ticket.');
      this.commentInput = '';
      this.supportTicketService.refreshAll();
      const fresh = this.tickets().find(t => t.id === ticket.id);
      if (fresh) this.selectedTicket.set(fresh);
    });
  }

  public assignToMe(): void {
    const ticket = this.selectedTicket();
    if (!ticket) return;

    const adminName = `${this.currentUser()?.fullName} (${this.isSuperAdmin() ? 'Super Admin' : 'Admin'})`;

    this.supportTicketService.updateTicketStatus(
      ticket.id,
      ticket.status === 'OPEN' ? 'IN_REVIEW' : ticket.status,
      ticket.resolutionNotes || '',
      adminName
    ).subscribe(updated => {
      if (updated) {
        this.toastService.success(`Ticket ${updated.ticketCode} assigned to ${adminName}.`);
        this.supportTicketService.refreshAll();
        const fresh = this.tickets().find(t => t.id === updated.id);
        this.selectedTicket.set(fresh || updated);
      }
    });
  }

  public onDetailFileSelected(event: Event): void {
    const ticket = this.selectedTicket();
    const input = event.target as HTMLInputElement;
    if (ticket && input.files && input.files.length > 0) {
      const file = input.files[0];
      this.supportTicketService.uploadAttachment(ticket.id, file).subscribe(updated => {
        if (updated) {
          this.toastService.success('Attachment uploaded to the ticket.');
          this.supportTicketService.refreshAll();
          const fresh = this.tickets().find(t => t.id === updated.id);
          this.selectedTicket.set(fresh || updated);
        }
      });
    }
  }

  public displayRole(role: string): string {
    if (!role) return 'Platform User';
    return role.replace('ROLE_', '').split('_').map(w => w.charAt(0) + w.slice(1).toLowerCase()).join(' ');
  }

  public formatDate(iso: string): string {
    if (!iso) return '—';
    const d = new Date(iso.includes('T') ? iso : iso.replace(' ', 'T'));
    if (isNaN(d.getTime())) return iso;
    const now = Date.now();
    const diff = now - d.getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'Just now';
    if (mins < 60) return `${mins}m ago`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    if (days === 1) return `Yesterday`;
    if (days < 7) return `${days} days ago`;
    return d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
  }
}