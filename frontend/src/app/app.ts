import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, skip, tap } from 'rxjs';
import { ToolbarComponent } from './toolbar/toolbar.component';
import { NotificationComponent } from './shared/notification/notification.component';
import { PreferenceService } from './preference/preference.service';
import { NotificationService } from './shared/notification/notification.service';
import { NotificationType } from './shared/dto/notification/Notification';
import { safeValue } from './shared/resource';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToolbarComponent, NotificationComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './app.scss',
  template: `
    <a class="skip-link" href="#main-content">Skip to content</a>
    <app-toolbar />
    <main id="main-content" tabindex="-1" class="page">
      <router-outlet />
    </main>
    <app-notifications />
  `,
})
export class App {
  private readonly router = inject(Router);
  private readonly preferenceService = inject(PreferenceService);
  private readonly notificationService = inject(NotificationService);
  private tmdbKeyChecked = false;
  private readonly preferenceResource = this.preferenceService.preferenceResource;

  constructor() {
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        tap((event) => this.redirectToPreferencesWhenTmdbKeyMissing(event)),
      )
      .subscribe();

    // Moves focus to the main content region on every client-side navigation
    // (skipping the very first, initial load, where focus is already at the
    // top of the document). Without this, keyboard/screen-reader users keep
    // whatever focus they had before navigating — usually the toolbar link
    // they just activated — and get no indication that new content loaded.
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        skip(1),
        tap(() => document.getElementById('main-content')?.focus()),
      )
      .subscribe();
  }

  private redirectToPreferencesWhenTmdbKeyMissing(event: NavigationEnd): void {
    if (this.tmdbKeyChecked || event.urlAfterRedirects === '/preferences') {
      return;
    }

    const preferences = safeValue(this.preferenceResource);
    if (preferences == null) {
      return;
    }

    this.tmdbKeyChecked = true;
    if (preferences.tmdbServiceKey) {
      return;
    }

    this.notificationService.notify({
      content: 'Set your TMDB service key in Preferences to start using Torenta.',
      type: NotificationType.WARNING,
    });
    void this.router.navigate(['/preferences']);
  }
}
