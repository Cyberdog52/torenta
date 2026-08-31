import { computed, effect, Injectable, signal } from '@angular/core';

export type ThemePreference = 'system' | 'light' | 'dark';

const STORAGE_KEY = 'torenta.theme';

/**
 * The app's Material theme is defined with `color-scheme: light dark`
 * (styles.scss), so every `--mat-sys-*` color is emitted as
 * `light-dark($light, $dark)` and normally follows the OS/browser
 * preference. Setting the `color-scheme` CSS property explicitly on
 * `<html>` overrides that preference for `light-dark()` resolution, which
 * is all this service needs to do to offer a manual light/dark switch.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly preference = signal<ThemePreference>(readStoredPreference());

  readonly current = computed(() => this.preference());

  constructor() {
    effect(() => {
      const value = this.preference();
      document.documentElement.style.colorScheme = value === 'system' ? 'light dark' : value;
      localStorage.setItem(STORAGE_KEY, value);
    });
  }

  set(preference: ThemePreference): void {
    this.preference.set(preference);
  }

  /** Cycles system -> light -> dark -> system, for a single toggle button. */
  cycle(): void {
    const next: Record<ThemePreference, ThemePreference> = {
      system: 'light',
      light: 'dark',
      dark: 'system',
    };
    this.preference.update((value) => next[value]);
  }
}

function readStoredPreference(): ThemePreference {
  const stored = localStorage.getItem(STORAGE_KEY);
  return stored === 'light' || stored === 'dark' || stored === 'system' ? stored : 'system';
}
