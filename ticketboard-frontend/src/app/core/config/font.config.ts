export interface FontOption {
  id: string;
  name: string;
  category: 'Modern Sans' | 'Corporate' | 'Monospace' | 'Serif Editorial' | 'Display & Tech';
  sampleText: string;
}

export interface FontSizeOption {
  id: string;
  name: string;
  sizePx: number;
  scale: string;
}

export interface FontColorOption {
  id: string;
  name: string;
  lightColor: string;
  darkColor: string;
}

export const CURATED_50_FONTS: FontOption[] = [
  // ─── 1. Modern Sans-Serif (15 Fonts) ──────────────────────────────────
  { id: 'Plus Jakarta Sans', name: 'Plus Jakarta Sans', category: 'Modern Sans', sampleText: 'Flagship executive delivery platform' },
  { id: 'Inter', name: 'Inter', category: 'Modern Sans', sampleText: 'Precision UI font engineered for dense dashboards' },
  { id: 'Roboto', name: 'Roboto', category: 'Modern Sans', sampleText: 'Classic clean Google material interface font' },
  { id: 'Outfit', name: 'Outfit', category: 'Modern Sans', sampleText: 'Geometric modern font with crisp letterforms' },
  { id: 'Poppins', name: 'Poppins', category: 'Modern Sans', sampleText: 'Geometric sans-serif with friendly open curves' },
  { id: 'Open Sans', name: 'Open Sans', category: 'Modern Sans', sampleText: 'Neutral & highly readable across desktop screens' },
  { id: 'Montserrat', name: 'Montserrat', category: 'Modern Sans', sampleText: 'Bold header typography inspired by urban signs' },
  { id: 'Lato', name: 'Lato', category: 'Modern Sans', sampleText: 'Warm, corporate sans-serif with rounded detail' },
  { id: 'Nunito', name: 'Nunito', category: 'Modern Sans', sampleText: 'Soft, rounded sans-serif for comfortable reading' },
  { id: 'Fira Sans', name: 'Fira Sans', category: 'Modern Sans', sampleText: 'Technical, clean sans-serif designed for legibility' },
  { id: 'Work Sans', name: 'Work Sans', category: 'Modern Sans', sampleText: 'Optimized for on-screen UI text & web applications' },
  { id: 'Manrope', name: 'Manrope', category: 'Modern Sans', sampleText: 'Modern semi-rounded geometric font family' },
  { id: 'DM Sans', name: 'DM Sans', category: 'Modern Sans', sampleText: 'Low-contrast geometric sans-serif for UI design' },
  { id: 'Public Sans', name: 'Public Sans', category: 'Modern Sans', sampleText: 'Strong, neutral interface font for governance' },
  { id: 'Urbanist', name: 'Urbanist', category: 'Modern Sans', sampleText: 'Low-contrast geometric typography for digital products' },

  // ─── 2. Corporate & Clean (12 Fonts) ─────────────────────────────────
  { id: 'Rubik', name: 'Rubik', category: 'Corporate', sampleText: 'Slightly rounded corners for executive interfaces' },
  { id: 'Sora', name: 'Sora', category: 'Corporate', sampleText: 'Distinct corporate display & body typography' },
  { id: 'Quicksand', name: 'Quicksand', category: 'Corporate', sampleText: 'Clean display font with rounded terminal ends' },
  { id: 'Barlow', name: 'Barlow', category: 'Corporate', sampleText: 'Slightly condensed font for dense grid layouts' },
  { id: 'Albert Sans', name: 'Albert Sans', category: 'Corporate', sampleText: 'Modern Scandinavian-inspired sans-serif' },
  { id: 'Figtree', name: 'Figtree', category: 'Corporate', sampleText: 'Clean and friendly font for enterprise apps' },
  { id: 'Space Grotesk', name: 'Space Grotesk', category: 'Corporate', sampleText: 'Tech-focused sans-serif derived from Space Mono' },
  { id: 'Lexend', name: 'Lexend', category: 'Corporate', sampleText: 'Engineered specifically to improve reading speed' },
  { id: 'Red Hat Display', name: 'Red Hat Display', category: 'Corporate', sampleText: 'Open source corporate Linux display font' },
  { id: 'Jost', name: 'Jost', category: 'Corporate', sampleText: 'Futura-inspired geometric corporate typography' },
  { id: 'Alegreya Sans', name: 'Alegreya Sans', category: 'Corporate', sampleText: 'Humanist sans-serif with a warm literary feel' },
  { id: 'Assistant', name: 'Assistant', category: 'Corporate', sampleText: 'Hebrew and Latin clean sans-serif typeface' },

  // ─── 3. Technical & Monospace (8 Fonts) ──────────────────────────────
  { id: 'JetBrains Mono', name: 'JetBrains Mono', category: 'Monospace', sampleText: 'Developer code & tabular financial numbers' },
  { id: 'Fira Code', name: 'Fira Code', category: 'Monospace', sampleText: 'Monospace font with programming ligatures' },
  { id: 'Source Code Pro', name: 'Source Code Pro', category: 'Monospace', sampleText: 'Adobe developer monospace font family' },
  { id: 'Space Mono', name: 'Space Mono', category: 'Monospace', sampleText: 'Fixed-width font with futuristic geometric charm' },
  { id: 'Inconsolata', name: 'Inconsolata', category: 'Monospace', sampleText: 'Clean programmer monospace for dense tables' },
  { id: 'Roboto Mono', name: 'Roboto Mono', category: 'Monospace', sampleText: 'Google Material monospace font for telemetry' },
  { id: 'IBM Plex Mono', name: 'IBM Plex Mono', category: 'Monospace', sampleText: 'Industrial IBM corporate monospace typography' },
  { id: 'Ubuntu Mono', name: 'Ubuntu Mono', category: 'Monospace', sampleText: 'Distinct Ubuntu Linux monospace typeface' },

  // ─── 4. Serif & Editorial (8 Fonts) ─────────────────────────────────
  { id: 'Playfair Display', name: 'Playfair Display', category: 'Serif Editorial', sampleText: 'High-contrast editorial serif headers' },
  { id: 'Merriweather', name: 'Merriweather', category: 'Serif Editorial', sampleText: 'Designed to be pleasant to read on screens' },
  { id: 'Lora', name: 'Lora', category: 'Serif Editorial', sampleText: 'Contemporary serif with roots in calligraphy' },
  { id: 'Cinzel', name: 'Cinzel', category: 'Serif Editorial', sampleText: 'Classic Roman proportioned executive serif' },
  { id: 'Newsreader', name: 'Newsreader', category: 'Serif Editorial', sampleText: 'On-screen continuous reading serif typeface' },
  { id: 'Bodoni Moda', name: 'Bodoni Moda', category: 'Serif Editorial', sampleText: 'Luxury fashion & high-end editorial serif' },
  { id: 'PT Serif', name: 'PT Serif', category: 'Serif Editorial', sampleText: 'Universal serif font for formal documentation' },
  { id: 'Crimson Pro', name: 'Crimson Pro', category: 'Serif Editorial', sampleText: 'Book-production serif font family' },

  // ─── 5. Display & Tech (7 Fonts) ───────────────────────────────────
  { id: 'Syne', name: 'Syne', category: 'Display & Tech', sampleText: 'Artistic display font for modern creative UI' },
  { id: 'Cabin', name: 'Cabin', category: 'Display & Tech', sampleText: 'Humanist sans-serif with modern proportions' },
  { id: 'Mulish', name: 'Mulish', category: 'Display & Tech', sampleText: 'Minimalist sans-serif designed for web & mobile' },
  { id: 'Overpass', name: 'Overpass', category: 'Display & Tech', sampleText: 'Inspired by US highway signage typography' },
  { id: 'Chakra Petch', name: 'Chakra Petch', category: 'Display & Tech', sampleText: 'Square Thai/Latin techno display font' },
  { id: 'Teko', name: 'Teko', category: 'Display & Tech', sampleText: 'Tall condensed headline font for dashboard stats' },
  { id: 'Rajdhani', name: 'Rajdhani', category: 'Display & Tech', sampleText: 'Modular squared sans-serif for UI cockpits' }
];

export const FONT_SIZES: FontSizeOption[] = [
  { id: 'compact', name: 'Compact (13px)', sizePx: 13, scale: '0.92' },
  { id: 'standard', name: 'Standard (14px)', sizePx: 14, scale: '1.0' },
  { id: 'comfortable', name: 'Comfortable (15px)', sizePx: 15, scale: '1.07' },
  { id: 'large', name: 'Large (16.5px)', sizePx: 16.5, scale: '1.18' },
  { id: 'executive', name: 'Executive (18px)', sizePx: 18, scale: '1.28' }
];

export const FONT_COLORS: FontColorOption[] = [
  { id: 'default', name: 'Theme Default', lightColor: '#0f172a', darkColor: '#f8fafc' },
  { id: 'high-contrast', name: 'Ultra High Contrast', lightColor: '#000000', darkColor: '#ffffff' },
  { id: 'soft-slate', name: 'Soft Charcoal Slate', lightColor: '#334155', darkColor: '#cbd5e1' },
  { id: 'cobalt-blue', name: 'Corporate Cobalt Navy', lightColor: '#0369a1', darkColor: '#e0f2fe' },
  { id: 'emerald-green', name: 'Executive Deep Emerald', lightColor: '#047857', darkColor: '#a7f3d0' },
  { id: 'onyx-black', name: 'Pure Onyx Black', lightColor: '#09090b', darkColor: '#ffffff' }
];
