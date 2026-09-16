export type ExportFormat = 'pdf' | 'csv' | 'excel' | 'word' | 'txt' | 'ppt';

export type ExportOrientation = 'portrait' | 'landscape';

export type ExportLayout = 'standard' | 'custom';

export const STATUS_OPTIONS: string[] = [
  'OPEN', 'IN_PROGRESS', 'UAT_EXIT', 'APPROVED', 'BLOCKED', 'COMPLETED', 'RESOLVED', 'CLOSED'
];

export interface ExportOptions {
  orientation?: ExportOrientation;
  layout?: ExportLayout;
}

export interface ExportColumn {
  key: string;
  label: string;
  align?: 'left' | 'right' | 'center';
  width?: number;
  format?: 'text' | 'number' | 'currency' | 'percent' | 'date' | 'boolean';
  sortable?: boolean;
  group?: string;
}

export interface ExportTable {
  title: string;
  description?: string;
  columns: ExportColumn[];
  rows: Record<string, any>[];
}

export interface ExportMeta {
  label: string;
  value: string;
}

export interface ExportDocument {
  title: string;
  subtitle?: string;
  meta?: ExportMeta[];
  summary?: ExportMeta[];
  sections: ExportTable[];
  notes?: string;
}

export interface ReportPreset {
  id: string;
  name: string;
  description: string;
  icon: string;
  categoryId: string;
  statusFilter: string;
  selectedColumnKeys: string[];
}
