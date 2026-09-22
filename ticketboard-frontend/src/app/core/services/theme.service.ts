import { Injectable, computed, signal } from '@angular/core';
import { DEFAULT_THEME_ID, getThemeById } from '../config/theme.config';

const STORAGE_KEY = 'tb_active_theme';
const SECTION_STORAGE_KEY = 'tb_section_styles';

export interface SectionStyles {
  header: string;
  sidebar: string;
  dashboard: string;
  footer: string;
  cards: string;
  accents: string;
}

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private readonly themeId = signal<string>(DEFAULT_THEME_ID);

  public readonly headerStyle = signal<string>('default');
  public readonly sidebarStyle = signal<string>('default');
  public readonly dashboardStyle = signal<string>('default');
  public readonly footerStyle = signal<string>('default');
  public readonly cardsStyle = signal<string>('default');
  public readonly accentsStyle = signal<string>('default');

  public readonly currentThemeId = computed(() => this.themeId());
  public readonly currentTheme = computed(() => getThemeById(this.themeId()));
  public readonly isDark = computed(() => {
    const dash = this.dashboardStyle();
    if (dash === 'dark-obsidian' || dash === 'slate-dark') return true;
    if (dash === 'white') return false;
    return this.currentTheme()?.mode === 'dark';
  });

  constructor() {
    this.init();
  }

  private init(): void {
    let id = DEFAULT_THEME_ID;
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored && getThemeById(stored)) {
        id = stored;
      }
    } catch {
      /* storage unavailable */
    }
    this.applyTheme(id, false);

    try {
      const storedSections = localStorage.getItem(SECTION_STORAGE_KEY);
      if (storedSections) {
        const parsed: SectionStyles = JSON.parse(storedSections);
        if (parsed.header) this.applyHeaderStyle(parsed.header, false);
        if (parsed.sidebar) this.applySidebarStyle(parsed.sidebar, false);
        if (parsed.dashboard) this.applyDashboardStyle(parsed.dashboard, false);
        if (parsed.footer) this.applyFooterStyle(parsed.footer, false);
        if (parsed.cards) this.applyCardsStyle(parsed.cards, false);
        if (parsed.accents) this.applyAccentsStyle(parsed.accents, false);
      }
    } catch {
      /* storage unavailable */
    }
  }

  public applyTheme(id: string, resetSections = true): void {
    const theme = getThemeById(id);
    if (!theme) return;

    this.themeId.set(id);
    if (typeof document !== 'undefined') {
      document.documentElement.dataset['theme'] = theme.id;
      document.documentElement.dataset['mode'] = theme.mode;
    }
    try {
      localStorage.setItem(STORAGE_KEY, theme.id);
    } catch {}

    if (resetSections) {
      this.resetSectionOverrides();
    }
  }

  public applyHeaderStyle(style: string, save = true): void {
    this.headerStyle.set(style);
    if (typeof document !== 'undefined') {
      document.documentElement.dataset['headerStyle'] = style;
    }
    if (save) this.saveSections();
  }

  public applySidebarStyle(style: string, save = true): void {
    this.sidebarStyle.set(style);
    if (typeof document !== 'undefined') {
      document.documentElement.dataset['sidebarStyle'] = style;
    }
    if (save) this.saveSections();
  }

  public applyDashboardStyle(style: string, save = true): void {
    this.dashboardStyle.set(style);
    if (typeof document !== 'undefined') {
      document.documentElement.dataset['dashboardStyle'] = style;
    }
    if (save) this.saveSections();
  }

  public applyCardsStyle(style: string, save = true): void {
    this.cardsStyle.set(style);
    if (typeof document !== 'undefined') {
      document.documentElement.dataset['cardsStyle'] = style;
    }
    if (save) this.saveSections();
  }

  public applyAccentsStyle(style: string, save = true): void {
    this.accentsStyle.set(style);
    if (typeof document !== 'undefined') {
      document.documentElement.dataset['accentsStyle'] = style;
    }
    if (save) this.saveSections();
  }

  public applyFooterStyle(style: string, save = true): void {
    this.footerStyle.set(style);
    if (typeof document !== 'undefined') {
      document.documentElement.dataset['footerStyle'] = style;
    }
    if (save) this.saveSections();
  }

  public resetSectionOverrides(): void {
    this.headerStyle.set('default');
    this.sidebarStyle.set('default');
    this.dashboardStyle.set('default');
    this.footerStyle.set('default');
    this.cardsStyle.set('default');
    this.accentsStyle.set('default');
    if (typeof document !== 'undefined') {
      delete document.documentElement.dataset['headerStyle'];
      delete document.documentElement.dataset['sidebarStyle'];
      delete document.documentElement.dataset['dashboardStyle'];
      delete document.documentElement.dataset['footerStyle'];
      delete document.documentElement.dataset['cardsStyle'];
      delete document.documentElement.dataset['accentsStyle'];
    }
    try {
      localStorage.removeItem(SECTION_STORAGE_KEY);
    } catch {}
  }

  public toggleDarkLight(): void {
    const currentModeDark = this.isDark();
    if (currentModeDark) {
      this.applyTheme('indigo', true);
    } else {
      this.applyTheme('zoho-dark', true);
    }
  }

  private saveSections(): void {
    try {
      const payload: SectionStyles = {
        header: this.headerStyle(),
        sidebar: this.sidebarStyle(),
        dashboard: this.dashboardStyle(),
        footer: this.footerStyle(),
        cards: this.cardsStyle(),
        accents: this.accentsStyle()
      };
      localStorage.setItem(SECTION_STORAGE_KEY, JSON.stringify(payload));
    } catch {}
  }
}