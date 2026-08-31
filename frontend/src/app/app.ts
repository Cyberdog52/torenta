import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { ToolbarComponent } from './toolbar/toolbar.component';
import { NotificationComponent } from './shared/notification/notification.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToolbarComponent, NotificationComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './app.scss',
  template: `
    <app-toolbar />
    <main class="page" [class.page--wide]="wide()">
      <router-outlet />
    </main>
    <app-notifications />
  `,
})
export class App {
  private readonly router = inject(Router);

  /**
   * Most pages read comfortably at the default max-width, but a route can
   * opt into a wider page container via `data: { wide: true }` (see
   * `app.routes.ts`) — currently only /search, which shows three side-by-side
   * result panels.
   */
  protected readonly wide = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map(() => isWideRoute(this.router.routerState.root)),
    ),
    { initialValue: false },
  );
}

function isWideRoute(route: ActivatedRoute): boolean {
  let current = route;
  while (current.firstChild) {
    current = current.firstChild;
  }
  return current.snapshot.data['wide'] === true;
}
