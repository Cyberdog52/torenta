import {Injectable} from '@angular/core';
import {Observable, Subject} from "rxjs";
import {Notification} from "../dto/notification/Notification";

@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  private notifications: Subject<Notification> = new Subject<Notification>();

  public getNotificationsObservable(): Observable<Notification> {
    return this.notifications.asObservable();
  }

  public addNotifications(notification: Notification) {
    this.notifications.next(notification)
  }

}
