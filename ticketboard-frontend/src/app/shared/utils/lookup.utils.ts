import { LookupData } from '../../core/models/api.models';

export function lookupLabel(items: LookupData[] | undefined | null, value: string | undefined | null, fallback = '—'): string {
  if (!value) return fallback;
  const found = items?.find((l) => l.value === value);
  return found ? found.label : value;
}

export function lookupColor(items: LookupData[] | undefined | null, value: string | undefined | null, fallback = '#64748B'): string {
  if (!value) return fallback;
  const found = items?.find((l) => l.value === value);
  return found?.colorCode || fallback;
}

export function groupLookup(items: LookupData[] | undefined | null): Record<string, LookupData[]> {
  const map: Record<string, LookupData[]> = {};
  (items || []).forEach((l) => {
    if (!map[l.category]) map[l.category] = [];
    if (l.isActive !== false) map[l.category].push(l);
  });
  return map;
}