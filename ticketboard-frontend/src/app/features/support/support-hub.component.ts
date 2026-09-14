import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { RaiseTicketDialogComponent } from './raise-ticket-dialog/raise-ticket-dialog.component';
import { SupportTicketService } from '../../core/services/support-ticket.service';

interface FAQItem {
  id: number;
  question: string;
  answer: string;
  category: string;
  expanded?: boolean;
}

@Component({
  selector: 'app-support-hub',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, MatIconModule, MatDialogModule],
  templateUrl: './support-hub.component.html',
  styleUrls: ['./support-hub.component.scss']
})
export class SupportHubComponent implements OnInit {
  public searchQuery = signal<string>('');
  public selectedCategory = signal<string>('ALL');

  public openTicketsCount = signal<number>(0);

  public faqs: FAQItem[] = [
    {
      id: 1,
      category: 'TASKS',
      question: 'How do I enable Excel-like Compact Grid View for Project Tasks?',
      answer: 'Navigate to any Project Detail page or standalone Tasks page (/tasks). Click the "Compact Grid" toggle button in the top right section header. This reduces row heights and displays compact task attributes.',
      expanded: true
    },
    {
      id: 2,
      category: 'TICKETS',
      question: 'Who receives the Support Tickets I raise to Super Admin or Admin?',
      answer: 'When you raise a ticket and select "Super Admin" or "Admin", your query is instantly queued in the Admin Support Queue (/admin/support-tickets). System administrators are notified immediately to review, update status, and post resolution notes.',
      expanded: false
    },
    {
      id: 3,
      category: 'GOVERNANCE',
      question: 'What do the 5 Core Delivery Questions on the Executive Dashboard mean?',
      answer: 'The Executive Dashboard aggregates real-time organizational KPIs across 5 core governance pillars: (1) What is Happening?, (2) Are Projects on Schedule?, (3) Is Delivery Efficient?, (4) Is Capacity Utilized?, and (5) What is Quality Status?',
      expanded: false
    },
    {
      id: 4,
      category: 'TIMELOGS',
      question: 'How do I log time against a specific SBI CR requirement or task?',
      answer: 'Click the "Log Time" button in the top header bar or press the timer icon on any task row. Select the Project, Work Item / Requirement, enter hours worked, and click Submit.',
      expanded: false
    },
    {
      id: 5,
      category: 'SECURITY',
      question: 'How are Role-Based Access Control (RBAC) permissions managed?',
      answer: 'Platform permissions are enforced per role (Super Admin, Admin, Project Owner, Project Manager, Lead, Developer). Super Admins and Admins can configure fine-grained permissions under Admin -> Roles & Permissions (/admin/roles).',
      expanded: false
    }
  ];

  constructor(
    private dialog: MatDialog,
    private supportTicketService: SupportTicketService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.supportTicketService.getTickets().subscribe(tickets => {
      this.openTicketsCount.set(tickets.filter(t => t.status === 'OPEN' || t.status === 'IN_REVIEW').length);
    });
  }

  public filteredFaqs(): FAQItem[] {
    const q = this.searchQuery().toLowerCase().trim();
    const cat = this.selectedCategory();

    return this.faqs.filter(faq => {
      const matchesCat = cat === 'ALL' || faq.category === cat;
      const matchesQuery = !q || faq.question.toLowerCase().includes(q) || faq.answer.toLowerCase().includes(q);
      return matchesCat && matchesQuery;
    });
  }

  public toggleFaq(faq: FAQItem): void {
    faq.expanded = !faq.expanded;
  }

  public openRaiseTicketDialog(targetRole: 'ROLE_SUPER_ADMIN' | 'ROLE_ADMIN' = 'ROLE_SUPER_ADMIN'): void {
    const dialogRef = this.dialog.open(RaiseTicketDialogComponent, {
      width: '680px',
      data: { targetRole }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.router.navigate(['/support/tickets']);
      }
    });
  }
}
