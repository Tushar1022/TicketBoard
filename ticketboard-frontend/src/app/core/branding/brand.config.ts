export const BRAND = {
  name: 'TicketBoard',
  fullName: 'TicketBoard by Tushar',
  tagline: 'Flagship Project Delivery & Work Management Platform',
  author: 'Tushar Shinde',
  authorTitle: 'Solution Architect & Platform Owner',
  company: 'TicketBoard Platform',
  version: 'Enterprise v3.0',
  initials: 'TB',
  supportEmail: 'support@ticketboard.com',
  website: 'www.ticketboard.com'
} as const;

export const REPORTS = {
  executive: 'Executive Command Center',
  project: 'Project Portfolio Status',
  requirement: 'Requirements & Scope Register',
  workItem: 'Work Items & Delivery Board',
  effort: 'Time & Effort Variance',
  capacity: 'Resource Capacity & Forecast',
  risk: 'Risk & Issue Register',
  release: 'Release & Deployment Pipeline',
  billing: 'Billing & Revenue'
} as const;

export function nowStamp(): string {
  const d = new Date();
  const pad = (n: number) => n.toString().padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export function todayStamp(): string {
  const d = new Date();
  const pad = (n: number) => n.toString().padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}