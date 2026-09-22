import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { THEMES, AppTheme } from '../../../core/config/theme.config';
import { CURATED_50_FONTS, FONT_COLORS, FONT_SIZES, FontOption } from '../../../core/config/font.config';
import { ThemeService } from '../../../core/services/theme.service';
import { TypographyService } from '../../../core/services/typography.service';
import { ToastService } from '../toast/toast.service';

@Component({
  selector: 'tb-theme-picker',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatTooltipModule],
  templateUrl: './theme-picker.component.html',
  styleUrls: ['./theme-picker.component.scss']
})
export class ThemePickerComponent {
  public readonly themeService = inject(ThemeService);
  public readonly typographyService = inject(TypographyService);
  private readonly toastService = inject(ToastService);

  public readonly activeTab = signal<'themes' | 'typography'>('themes');

  public readonly themes = THEMES;
  public readonly fontOptions = CURATED_50_FONTS;
  public readonly fontSizes = FONT_SIZES;
  public readonly fontColors = FONT_COLORS;

  public readonly fontCategory = signal<string>('ALL');
  public readonly fontSearch = signal<string>('');

  public readonly filteredFonts = computed<FontOption[]>(() => {
    const q = this.fontSearch().toLowerCase().trim();
    const cat = this.fontCategory();
    return this.fontOptions.filter(f => {
      const matchCat = cat === 'ALL' || f.category === cat;
      const matchQ = !q || f.name.toLowerCase().includes(q) || f.category.toLowerCase().includes(q);
      return matchCat && matchQ;
    });
  });

  public readonly currentThemeId = this.themeService.currentThemeId;

  public byId(id: string): AppTheme | undefined {
    return THEMES.find((t) => t.id === id);
  }

  public groups(): { label: string; themes: AppTheme[] }[] {
    const groups: { label: string; themes: AppTheme[] }[] = [];
    for (const theme of this.themes) {
      const g = groups.find((x) => x.label === theme.group);
      if (g) {
        g.themes.push(theme);
      } else {
        groups.push({ label: theme.group, themes: [theme] });
      }
    }
    return groups;
  }

  public select(theme: AppTheme): void {
    this.themeService.applyTheme(theme.id, true);
    this.toastService.success(`Applied "${theme.name}" theme.`);
  }

  public isActive(id: string): boolean {
    return this.currentThemeId() === id;
  }

  public setHeaderStyle(style: string): void {
    this.themeService.applyHeaderStyle(style);
    this.toastService.info(`Header style set to ${style.replace('-', ' ')}.`);
  }

  public setSidebarStyle(style: string): void {
    this.themeService.applySidebarStyle(style);
    this.toastService.info(`Sidebar style set to ${style.replace('-', ' ')}.`);
  }

  public setDashboardStyle(style: string): void {
    this.themeService.applyDashboardStyle(style);
    this.toastService.info(`Dashboard canvas set to ${style.replace('-', ' ')}.`);
  }

  public setFooterStyle(style: string): void {
    this.themeService.applyFooterStyle(style);
    this.toastService.info(`Footer style set to ${style.replace('-', ' ')}.`);
  }

  public setCardsStyle(style: string): void {
    this.themeService.applyCardsStyle(style);
    this.toastService.info(`Cards & Tables style set to ${style.replace('-', ' ')}.`);
  }

  public setAccentsStyle(style: string): void {
    this.themeService.applyAccentsStyle(style);
    this.toastService.info(`Charts & Status Accents style set to ${style.replace('-', ' ')}.`);
  }

  public resetOverrides(): void {
    this.themeService.resetSectionOverrides();
    this.toastService.info('Reset all section overrides to default theme.');
  }

  // ─── Typography Methods ────────────────────────────────────────────────
  public readonly customPreviewText = signal<string>('TicketBoard Enterprise Agile Management System 2026');

  public previewFontOnHover(fontName: string): void {
    this.typographyService.ensureFontLoaded(fontName);
  }

  public selectBodyFont(fontName: string): void {
    this.typographyService.applyBodyFont(fontName);
    this.toastService.success(`Applied "${fontName}" as Body Font across the application.`);
  }

  public selectHeadingFont(fontName: string): void {
    this.typographyService.applyHeadingFont(fontName);
    this.toastService.success(`Applied "${fontName}" as Heading Font across the application.`);
  }

  public selectBothFonts(fontName: string): void {
    this.typographyService.applyBothFonts(fontName);
    this.toastService.success(`Applied "${fontName}" to Entire Application (Body & Heading).`);
  }

  public selectFontSize(sizeId: string): void {
    this.typographyService.applyFontSize(sizeId);
    this.toastService.info(`Font size scale updated to ${sizeId}.`);
  }

  public selectFontColor(colorId: string): void {
    this.typographyService.applyFontColor(colorId);
    this.toastService.info(`Font color preset updated.`);
  }

  public resetTypography(): void {
    this.typographyService.resetTypography();
    this.toastService.info('Reset typography & font scaling to default.');
  }
}