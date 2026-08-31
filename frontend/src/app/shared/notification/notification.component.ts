import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { NotificationService } from './notification.service';
import { Notification, NotificationType } from '../dto/notification/Notification';

/** Errors are kept until dismissed, but capped so a flaky backend can't turn this into an infinite scroll. */
const MAX_ERRORS = 10;

@Component({
  selector: 'app-notifications',
  imports: [MatButtonModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @for (error of errors(); track error) {
      <p class="error">
        {{ error.content }}
        <button mat-icon-button type="button" aria-label="Dismiss error" (click)="dismiss(error)">
          <mat-icon>close</mat-icon>
        </button>
      </p>
    }
  `,
  styleUrl: './notification.component.scss',
})
export class NotificationComponent {
  private readonly notificationService = inject(NotificationService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly errors = signal<Notification[]>([]);

  constructor() {
    this.notificationService.notifications$.pipe(takeUntilDestroyed()).subscribe((notification) => {
      if (notification.type === NotificationType.ERROR) {
        this.errors.update((errors) => [...errors, notification].slice(-MAX_ERRORS));
      } else {
        this.snackBar.open(notification.content, undefined, { duration: 3000 });
      }
    });
  }

  protected dismiss(notification: Notification): void {
    this.errors.update((errors) => errors.filter((error) => error !== notification));
  }
}
