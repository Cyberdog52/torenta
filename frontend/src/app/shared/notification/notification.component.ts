import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
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
    <div role="alert" aria-live="assertive" aria-atomic="true">
      @for (error of errors(); track error) {
        <p class="error">
          {{ error.content }}
          <button mat-icon-button type="button" aria-label="Dismiss error" (click)="dismiss(error)">
            <mat-icon>close</mat-icon>
          </button>
        </p>
      }
    </div>
  `,
  styleUrl: './notification.component.scss',
})
export class NotificationComponent {
  private readonly notificationService = inject(NotificationService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly errors = signal<Notification[]>([]);

  constructor() {
    effect(() => {
      const notification = this.notificationService.notification();
      if (notification == null) {
        return;
      }
      if (notification.type === NotificationType.ERROR) {
        this.errors.update((errors) => [...errors, notification].slice(-MAX_ERRORS));
        return;
      }
      if (notification.type === NotificationType.WARNING) {
        this.snackBar.open(notification.content, notification.action?.label, {
          duration: 10000,
          politeness: 'assertive',
          panelClass: ['warning-snackbar'],
        });
        return;
      }
      const snackBarRef = this.snackBar.open(notification.content, notification.action?.label, {
        duration: 5000,
      });
      if (notification.action) {
        snackBarRef.onAction().subscribe(() => notification.action?.onClick());
      }
    });
  }

  protected dismiss(notification: Notification): void {
    this.errors.update((errors) => errors.filter((error) => error !== notification));
  }
}
