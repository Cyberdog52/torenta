import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-page-not-found',
  imports: [RouterLink, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './page-not-found.component.scss',
  template: `
    <h1>Page not found</h1>
    <img src="404.jpg" alt="" aria-hidden="true" />
    <button matButton="elevated" routerLink="/">Return to base</button>
  `,
})
export class PageNotFoundComponent {}
