import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { RaiseTicketDialogComponent } from '../../support/raise-ticket-dialog/raise-ticket-dialog.component';
import { SupportTicketService } from '../../../core/services/support-ticket.service';
import { AuthService } from '../../../core/services/auth.service';
import { SupportTicket, TicketStatus, SupportCategory } from '../../../core/models/api.models';
import { ToastService } from '../../../shared/components/toast/toast.service';

@Component({
  selector: 'app-admin-support-tickets',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, MatIconModule, MatTooltipModule, MatDialogModule],
  templateUrl: './admin-support-tickets.component.html',
  styleUrls: ['./admin-support-tickets.component.scss']
})
export class AdminSupportTicketsComponent implements OnInit {
  public selectedTicket = signal<SupportTicket | null>(null);

  public searchQuery = signal<string>('');
  public selectedStatus = signal<string>('ALL');
  public selectedPriority = signal<string>('ALL');
  public selectedCategory = signal<string>('ALL');
  public selectedQueue = signal<string>('ALL');

  public statusOptions: TicketStatus[] = ['OPEN', 'IN_REVIEW', 'RESOLVED', 'CLOSED'];
  public categoryOptions: SupportCategory[] = ['SYSTEM_DEFECT', 'ACCESS_REQUEST', 'DATA_QUERY', 'PERFORMANCE_ISSUE', 'BILLING_SLA', 'CUSTOM_ISSUE', 'OTHER'];

  public resolutionNotesInput = '';
  public statusUpdateInput: TicketStatus = 'RESOLVED';
  public commentInput = '';
  public toastMessage = signal<string>('');

  public currentUser = computed(() => this.authService.currentUser());
  public isSuperAdmin = computed(() => (this.currentUser()?.roles || []).includes('ROLE_SUPER_ADMIN'));

  public tickets = this.supportTicketService.tickets;
  public isLoading = computed(() => this.supportTicketService.isLoading());
  public stats = this.supportTicketService.stats;

  public filteredTickets = computed<SupportTicket[]>(() => {
    const q = this.searchQuery().toLowerCase().trim();
    const status = this.selectedStatus();
    const priority = this.selectedPriority();
    const category = this.selectedCategory();
    const queue = this.selectedQueue();

    return this.tickets().filter(t => {
      const matchesStatus = status === 'ALL' || t.status === status;
      const matchesPriority = priority === 'ALL' || t.priority === priority;
      const matchesCategory = category === 'ALL' || t.category === category;
      const matchesQueue = queue === 'ALL' || t.targetRole === queue;
      const matchesSearch =
        !q ||
        t.ticketCode.toLowerCase().includes(q) ||
        t.subject.toLowerCase().includes(q) ||
        t.createdByName.toLowerCase().includes(q) ||
        t.createdByEmail.toLowerCase().includes(q) ||
        t.category.toLowerCase().includes(q);

      return matchesStatus && matchesPriority && matchesCategory && matchesQueue && matchesSearch;
    });
  });

  public kpiCards = computed(() => {
    const list = this.tickets();
    const s = this.stats();
    return [
      { key: 'total', label: 'Total Tickets', value: s?.totalTickets ?? list.length, icon: 'confirmation_number', tone: 'indigo' },
      { key: 'open', label: 'Open', value: s?.openTickets ?? list.filter(t => t.status === 'OPEN').length, icon: 'schedule', tone: 'amber' },
      { key: 'review', label: 'In Review', value: s?.inReviewTickets ?? list.filter(t => t.status === 'IN_REVIEW').length, icon: 'manage_search', tone: 'blue' },
      { key: 'urgent', label: 'Urgent', value: s?.urgentTickets ?? list.filter(t => t.priority === 'URGENT' && t.status !== 'RESOLVED' && t.status !== 'CLOSED').length, icon: 'error', tone: 'red' },
      { key: 'resolved', label: 'Resolved', value: s?.resolvedTickets ?? list.filter(t => t.status === 'RESOLVED').length, icon: 'verified', tone: 'green' },
      { key: 'unassigned', label: 'Unassigned', value: s?.unassignedTickets ?? list.filter(t => !t.assignedToName).length, icon: 'person_off', tone: 'slate' }
    ];
  });

  public categoryBreakdown = computed(() => {
    const entries = Object.entries(this.stats()?.byCategory ?? {});
    if (entries.length === 0) {
      const map = new Map<SupportCategory, number>();
      this.tickets().forEach(t => map.set(t.category, (map.get(t.category) || 0) + 1));
      return [...map.entries()].map(([category, count]) => ({ category, count, pct: this.tickets().length ? Math.round((count / this.tickets().length) * 100) : 0 }));
    }
    const total = entries.reduce((acc, [, v]) => acc + v, 0);
    return entries
      .map(([category, count]) => ({ category, count, pct: total ? Math.round((count / total) * 100) : 0 }))
      .sort((a, b) => b.count - a.count);
  });

  public queueSplit = computed(() => {
    const superAdmin = this.stats()?.byTargetRole?.ROLE_SUPER_ADMIN ?? this.tickets().filter(t => t.targetRole === 'ROLE_SUPER_ADMIN').length;
    const admin = this.stats()?.byTargetRole?.ROLE_ADMIN ?? this.tickets().filter(t => t.targetRole === 'ROLE_ADMIN').length;
    const total = superAdmin + admin || 1;
    return {
      superAdmin,
      admin,
      superPct: Math.round((superAdmin / total) * 100),
      adminPct: Math.round((admin / total) * 100)
    };
  });

  constructor(
    public supportTicketService: SupportTicketService,
    public authService: AuthService,
    private dialog: MatDialog,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.supportTicketService.refreshAll();
    this.supportTicketService.refreshStats();

    // Auto-select the highest priority unresolved ticket
    const prioritized = [...this.tickets()].sort((a, b) => {
      const prio = { URGENT: 3, HIGH: 2, MEDIUM: 1, LOW: 0 };
      return (prio[b.priority] - prio[a.priority]) || (a.status === 'OPEN' ? -1 : 1);
    });
    const first = prioritized.find(t => t.status === 'OPEN' || t.status === 'IN_REVIEW') || prioritized[0];
    if (first) this.selectTicket(first);
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
    this.selectedQueue.set('ALL');
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
        this.supportTicketService.refreshAll();
        this.supportTicketService.refreshStats();
        const fresh = this.tickets().find(t => t.id === updated.id);
        this.selectedTicket.set(fresh || updated);
        this.showToast(`Success — ${updated.ticketCode} moved to ${updated.status.replace('_', ' ')}.`);
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
        this.supportTicketService.refreshAll();
        this.supportTicketService.refreshStats();
        const fresh = this.tickets().find(t => t.id === updated.id);
        this.selectedTicket.set(fresh || updated);
        this.showToast(`Assigned ${updated.ticketCode} to ${adminName}.`);
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
          this.supportTicketService.refreshAll();
          this.supportTicketService.refreshStats();
          const fresh = this.tickets().find(t => t.id === updated.id);
          this.selectedTicket.set(fresh || updated);
          this.showToast('Attachment uploaded successfully.');
        }
      });
    }
  }

  public showToast(msg: string): void {
    this.toastMessage.set(msg);
    this.toastService.success(msg);
    setTimeout(() => this.toastMessage.set(''), 2600);
  }

  public displayRole(role: string): string {
    if (!role) return 'Platform User';
    return role
      .replace('ROLE_', '')
      .split('_')
      .map(w => w.charAt(0) + w.slice(1).toLowerCase())
      .join(' ');
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
    if (days === 1) return 'Yesterday';
    if (days < 7) return `${days} days ago`;
    return d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
  }
}