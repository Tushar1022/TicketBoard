import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RaiseTicketDialogComponent } from './raise-ticket-dialog/raise-ticket-dialog.component';
import { SupportTicketService } from '../../core/services/support-ticket.service';
import { AuthService } from '../../core/services/auth.service';

interface FAQItem {
  id: number;
  question: string;
  answer: string;
  category: string;
  expanded?: boolean;
}

interface KbCategory {
  id: string;
  icon: string;
  title: string;
  description: string;
  accent: string;
}

@Component({
  selector: 'app-support-hub',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, MatIconModule, MatDialogModule, MatTooltipModule],
  templateUrl: './support-hub.component.html',
  styleUrls: ['./support-hub.component.scss']
})
export class SupportHubComponent implements OnInit {
  public searchQuery = signal<string>('');
  public selectedCategory = signal<string>('ALL');

  public openTicketsCount = computed(() => this.supportTicketService.openTicketsCount());
  public isAdminUser = computed(() => this.authService.isAdmin());

  public kbCategories: KbCategory[] = [
    {
      id: 'getting-started',
      icon: 'rocket_launch',
      title: 'Getting Started',
      description: 'Navigate dashboards, project detail workspaces and executive delivery telemetry.',
      accent: 'indigo'
    },
    {
      id: 'tasks',
      icon: 'task_alt',
      title: 'Tasks & Delivery Board',
      description: 'Master Kanban, list views, inline editing, dependencies and blocker triage.',
      accent: 'blue'
    },
    {
      id: 'governance',
      icon: 'fact_check',
      title: 'SLA & Milestone Governance',
      description: 'Track DEV, SIT, UAT exit gates, delivery health, risks and acceptance criteria.',
      accent: 'gold'
    },
    {
      id: 'timelogs',
      icon: 'schedule',
      title: 'Time & Capacity Planning',
      description: 'Log effort, submit weekly timesheets and review utilization forecasts.',
      accent: 'green'
    },
    {
      id: 'reports',
      icon: 'analytics',
      title: 'Reports & Exports',
      description: 'Build custom reports and export branded PDF, Excel and PowerPoint decks.',
      accent: 'purple'
    },
    {
      id: 'security',
      icon: 'admin_panel_settings',
      title: 'RBAC & Security',
      description: 'Understand role-based permissions, access governance and audit trails.',
      accent: 'teal'
    }
  ];

  public faqs: FAQItem[] = [
    {
      id: 1,
      category: 'TASKS',
      question: 'How do I enable the Excel-like compact grid view for project tasks?',
      answer: 'Navigate to any Project Detail page or the standalone Tasks page. Click the "Compact Grid" toggle in the top-right section header to reduce row heights and display compact task attributes.',
      expanded: true
    },
    {
      id: 2,
      category: 'TICKETS',
      question: 'Who receives the support tickets I raise to Super Admin or Admin?',
      answer: 'Tickets raised to Super Admin or Admin are queued instantly in the Admin Support Governance console (/admin/support-tickets). Administrators are notified to review, update status and post resolution notes in real time.',
      expanded: false
    },
    {
      id: 3,
      category: 'GOVERNANCE',
      question: 'What do the 5 core delivery questions on the Executive Dashboard mean?',
      answer: 'The Executive Dashboard aggregates real-time organisational KPIs across 5 governance pillars: What is happening?, What is delayed & at risk?, What is coming?, Who is overloaded?, and Are we on schedule?.',
      expanded: false
    },
    {
      id: 4,
      category: 'TIMELOGS',
      question: 'How do I log time against a requirement or task?',
      answer: 'Click the "Log Time" button in the top header bar, or the timer icon on any task row. Select the project, requirement or work item, enter the hours and click Submit. Timesheets capture Monday–Sunday and route to your manager for approval.',
      expanded: false
    },
    {
      id: 5,
      category: 'REPORTS',
      question: 'How do I export an executive deck or PDF report?',
      answer: 'Open the Reports Hub (/reports), choose the category, select columns and click Export. You can generate branded PDF, Excel or PowerPoint deliverables with live database calculations.',
      expanded: false
    },
    {
      id: 6,
      category: 'SECURITY',
      question: 'How are role-based access control (RBAC) permissions managed?',
      answer: 'Permissions are enforced per role (Super Admin, Admin, Project Owner, Project Manager, Team Lead, Analyst, Developer). Admins configure granular authorities under Admin → Roles & Permissions.',
      expanded: false
    }
  ];

  constructor(
    private dialog: MatDialog,
    public supportTicketService: SupportTicketService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.supportTicketService.refreshAll();
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