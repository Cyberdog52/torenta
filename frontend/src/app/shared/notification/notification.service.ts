import { Injectable, signal } from '@angular/core';
import { Notification } from '../dto/notification/Notification';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly lastNotification = signal<Notification | null>(null);

  /** The most recently raised notification, or `null` before the first one. */
  readonly notification = this.lastNotification.asReadonly();

  notify(notification: Notification): void {
    this.lastNotification.set(notification);
  }
}
