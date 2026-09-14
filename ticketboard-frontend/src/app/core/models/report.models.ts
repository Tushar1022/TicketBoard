export type ExportFormat = 'pdf' | 'csv' | 'excel' | 'word' | 'txt' | 'ppt';

export interface ExportColumn {
  key: string;
  label: string;
  align?: 'left' | 'right' | 'center';
  width?: number;
  format?: 'text' | 'number' | 'currency' | 'percent' | 'date' | 'boolean';
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
