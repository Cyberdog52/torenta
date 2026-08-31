import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { Notification } from '../dto/notification/Notification';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly notifications = new Subject<Notification>();

  readonly notifications$: Observable<Notification> = this.notifications.asObservable();

  notify(notification: Notification): void {
    this.notifications.next(notification);
  }
}
