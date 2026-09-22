import { Injectable, signal, computed } from '@angular/core';
import { CURATED_50_FONTS, FONT_COLORS, FONT_SIZES, FontOption } from '../config/font.config';

const TYPOGRAPHY_STORAGE_KEY = 'tb_typography_config';

export interface TypographyConfig {
  bodyFont: string;
  headingFont: string;
  fontSize: string;
  fontColor: string;
}

@Injectable({
  providedIn: 'root'
})
export class TypographyService {
  public readonly bodyFont = signal<string>('Plus Jakarta Sans');
  public readonly headingFont = signal<string>('Plus Jakarta Sans');
  public readonly fontSize = signal<string>('standard');
  public readonly fontColor = signal<string>('default');

  private readonly loadedFonts = new Set<string>();

  constructor() {
    this.init();
  }

  private init(): void {
    let config: TypographyConfig = {
      bodyFont: 'Plus Jakarta Sans',
      headingFont: 'Plus Jakarta Sans',
      fontSize: 'standard',
      fontColor: 'default'
    };

    try {
      const stored = localStorage.getItem(TYPOGRAPHY_STORAGE_KEY);
      if (stored) {
        config = { ...config, ...JSON.parse(stored) };
      }
    } catch {
      /* storage unavailable */
    }

    this.applyBodyFont(config.bodyFont, false);
    this.applyHeadingFont(config.headingFont, false);
    this.applyFontSize(config.fontSize, false);
    this.applyFontColor(config.fontColor, false);
  }

  public applyBodyFont(fontName: string, save = true): void {
    this.ensureFontLoaded(fontName);
    this.bodyFont.set(fontName);
    if (typeof document !== 'undefined') {
      document.documentElement.style.setProperty('--body-font-family', `'${fontName}', sans-serif`);
    }
    if (save) this.saveConfig();
  }

  public applyHeadingFont(fontName: string, save = true): void {
    this.ensureFontLoaded(fontName);
    this.headingFont.set(fontName);
    if (typeof document !== 'undefined') {
      document.documentElement.style.setProperty('--heading-font-family', `'${fontName}', sans-serif`);
    }
    if (save) this.saveConfig();
  }

  public applyBothFonts(fontName: string, save = true): void {
    this.ensureFontLoaded(fontName);
    this.bodyFont.set(fontName);
    this.headingFont.set(fontName);
    if (typeof document !== 'undefined') {
      document.documentElement.style.setProperty('--body-font-family', `'${fontName}', sans-serif`);
      document.documentElement.style.setProperty('--heading-font-family', `'${fontName}', sans-serif`);
    }
    if (save) this.saveConfig();
  }

  public applyFontSize(sizeId: string, save = true): void {
    this.fontSize.set(sizeId);
    const found = FONT_SIZES.find(s => s.id === sizeId) || FONT_SIZES[1];
    if (typeof document !== 'undefined') {
      document.documentElement.style.setProperty('--base-font-size', `${found.sizePx}px`);
      document.documentElement.dataset['fontSize'] = sizeId;
    }
    if (save) this.saveConfig();
  }

  public applyFontColor(colorId: string, save = true): void {
    this.fontColor.set(colorId);
    const found = FONT_COLORS.find(c => c.id === colorId) || FONT_COLORS[0];
    if (typeof document !== 'undefined') {
      if (colorId === 'default') {
        delete document.documentElement.dataset['fontColor'];
        document.documentElement.style.removeProperty('--custom-text-main');
      } else {
        document.documentElement.dataset['fontColor'] = colorId;
        document.documentElement.style.setProperty('--custom-text-main-light', found.lightColor);
        document.documentElement.style.setProperty('--custom-text-main-dark', found.darkColor);
      }
    }
    if (save) this.saveConfig();
  }

  public resetTypography(): void {
    this.applyBodyFont('Plus Jakarta Sans', false);
    this.applyHeadingFont('Plus Jakarta Sans', false);
    this.applyFontSize('standard', false);
    this.applyFontColor('default', true);
  }

  public ensureFontLoaded(fontName: string): void {
    if (typeof document === 'undefined' || !fontName || fontName === 'system-ui') return;
    const fontId = fontName.replace(/\s+/g, '+');
    if (this.loadedFonts.has(fontId)) return;

    this.loadedFonts.add(fontId);

    // Primary link for default weights (Guarantees 200 OK across all Google Font categories)
    const linkBase = document.createElement('link');
    linkBase.rel = 'stylesheet';
    linkBase.href = `https://fonts.googleapis.com/css2?family=${fontId}&display=swap`;
    document.head.appendChild(linkBase);

    // Extended weights link for rich weight styling (300-800)
    const linkWeights = document.createElement('link');
    linkWeights.rel = 'stylesheet';
    linkWeights.href = `https://fonts.googleapis.com/css2?family=${fontId}:wght@300;400;500;600;700;800&display=swap`;
    document.head.appendChild(linkWeights);
  }

  private saveConfig(): void {
    try {
      const payload: TypographyConfig = {
        bodyFont: this.bodyFont(),
        headingFont: this.headingFont(),
        fontSize: this.fontSize(),
        fontColor: this.fontColor()
      };
      localStorage.setItem(TYPOGRAPHY_STORAGE_KEY, JSON.stringify(payload));
    } catch {}
  }
}
