import { WorkItemStatus } from '../models/api.models';

export interface WorkItemStatusConfig {
  value: WorkItemStatus;
  label: string;
  badgeClass: string;
  category: 'OPEN' | 'CLOSED';
  boardColumn?: boolean;
}

export const STATUS_CATALOG: WorkItemStatusConfig[] = [
  { value: 'TODO', label: 'To Do', badgeClass: 'badge-slate', category: 'OPEN', boardColumn: true },
  { value: 'IN_ANALYSIS', label: 'In Analysis', badgeClass: 'badge-blue', category: 'OPEN', boardColumn: true },
  { value: 'DEV_IN_PROGRESS', label: 'Dev In Progress', badgeClass: 'badge-indigo', category: 'OPEN', boardColumn: true },
  { value: 'DEV_EXIT', label: 'Dev Exit', badgeClass: 'badge-purple', category: 'OPEN', boardColumn: true },
  { value: 'IN_REVIEW', label: 'In Review', badgeClass: 'badge-violet', category: 'OPEN', boardColumn: true },
  { value: 'SIT_IN_PROGRESS', label: 'SIT In Progress', badgeClass: 'badge-amber', category: 'OPEN', boardColumn: true },
  { value: 'SIT_EXIT', label: 'SIT Exit', badgeClass: 'badge-orange', category: 'OPEN', boardColumn: true },
  { value: 'UAT_IN_PROGRESS', label: 'UAT In Progress', badgeClass: 'badge-cyan', category: 'OPEN', boardColumn: true },
  { value: 'UAT_EXIT', label: 'UAT Exit', badgeClass: 'badge-emerald', category: 'OPEN', boardColumn: true },
  { value: 'PRE_PROD', label: 'Pre-Production', badgeClass: 'badge-teal', category: 'OPEN', boardColumn: true },
  { value: 'GO_LIVE', label: 'Go-Live / Prod', badgeClass: 'badge-green', category: 'OPEN', boardColumn: true },
  { value: 'BLOCKED', label: 'Blocked', badgeClass: 'badge-red', category: 'OPEN', boardColumn: true },
  { value: 'IN_PROGRESS', label: 'In Progress', badgeClass: 'badge-indigo', category: 'OPEN' },
  { value: 'TESTING', label: 'Testing', badgeClass: 'badge-amber', category: 'OPEN' },
  { value: 'COMPLETED', label: 'Completed', badgeClass: 'badge-teal', category: 'CLOSED', boardColumn: true },
  { value: 'CLOSED', label: 'Closed', badgeClass: 'badge-slate', category: 'CLOSED' },
  { value: 'Go-Live to be planned', label: 'Go-Live To Be Planned', badgeClass: 'badge-cyan', category: 'OPEN' },
  { value: 'BRD', label: 'BRD', badgeClass: 'badge-blue', category: 'OPEN' }
];

const CATALOG_BY_VALUE = new Map<WorkItemStatus, WorkItemStatusConfig>(
  STATUS_CATALOG.map((cfg) => [cfg.value, cfg])
);

export function statusConfig(status?: WorkItemStatus | null): WorkItemStatusConfig {
  if (!status) return { value: status as WorkItemStatus, label: '—', badgeClass: 'badge-slate', category: 'OPEN' };
  return CATALOG_BY_VALUE.get(status) || { value: status, label: status.replace(/_/g, ' '), badgeClass: 'badge-slate', category: 'OPEN' };
}

export function statusLabel(status?: WorkItemStatus | null): string {
  return statusConfig(status).label;
}

export function statusBadge(status?: WorkItemStatus | null): string {
  return statusConfig(status).badgeClass;
}

export function isOpenStatus(status?: WorkItemStatus | null): boolean {
  return statusConfig(status).category === 'OPEN';
}

export function boardStatuses(): WorkItemStatusConfig[] {
  return STATUS_CATALOG.filter((cfg) => cfg.boardColumn);
}

export function statusOptions(): WorkItemStatusConfig[] {
  return STATUS_CATALOG;
}