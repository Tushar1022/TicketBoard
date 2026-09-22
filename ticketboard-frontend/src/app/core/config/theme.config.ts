export type ThemeMode = 'light' | 'dark';

export interface AppTheme {
  id: string;
  name: string;
  description: string;
  mode: ThemeMode;
  premium: boolean;
  group: 'Standard' | 'Zoho Edition' | 'Dark Themes';
  swatches: string[];
}

export const DEFAULT_THEME_ID = 'indigo';

export const THEMES: AppTheme[] = [
  {
    id: 'indigo',
    name: 'Classic Indigo',
    description: 'Professional slate surfaces, dark navy rail, and vibrant indigo controls.',
    mode: 'light',
    premium: false,
    group: 'Standard',
    swatches: ['#4f46e5', '#f8fafc', '#0f172a', '#ffffff', '#0ea5e9']
  },
  {
    id: 'zoho-dark',
    name: 'Zoho Obsidian (Black & White)',
    description: 'High-contrast dark mode like Zoho with charcoal black panels and crisp white text.',
    mode: 'dark',
    premium: false,
    group: 'Zoho Edition',
    swatches: ['#2563eb', '#09090b', '#18181b', '#27272a', '#ffffff']
  },
  {
    id: 'zoho-light',
    name: 'Zoho Corporate White',
    description: 'Clean enterprise white workspace with deep blue header and distinct section cards.',
    mode: 'light',
    premium: false,
    group: 'Zoho Edition',
    swatches: ['#0284c7', '#f1f5f9', '#1e293b', '#ffffff', '#38bdf8']
  },
  {
    id: 'emerald',
    name: 'Emerald Executive',
    description: 'Growth-focused mint surfaces with dark forest navigation rail and emerald accents.',
    mode: 'light',
    premium: false,
    group: 'Standard',
    swatches: ['#059669', '#f4faf6', '#04291e', '#ffffff', '#10b981']
  },
  {
    id: 'slate-dark',
    name: 'Midnight Slate',
    description: 'Deep navy-slate dark cockpit with electric cyan accents and dark card elevation.',
    mode: 'dark',
    premium: false,
    group: 'Dark Themes',
    swatches: ['#0ea5e9', '#0f172a', '#1e293b', '#334155', '#38bdf8']
  }
];

export function getThemeById(id: string): AppTheme | undefined {
  return THEMES.find((t) => t.id === id);
}

export function isThemeDark(id: string): boolean {
  return getThemeById(id)?.mode === 'dark';
}