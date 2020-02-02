import {Component, OnDestroy, OnInit} from '@angular/core';
import {NotificationService} from "./notification.service";
import {Subject} from "rxjs";
import {Notification, NotificationType} from "../dto/notification/Notification";
import {takeUntil} from "rxjs/operators";
import {MatSnackBar} from "@angular/material";


@Component({
  selector: 'app-notifications',
  templateUrl: './notification.component.html',
  styleUrls: ['./notification.component.scss']
})
export class NotificationComponent implements OnInit, OnDestroy {
  private notifications: Notification[] = [];
  destroy$: Subject<boolean> = new Subject<boolean>();

  constructor(private notificationService: NotificationService,
              private _snackBar: MatSnackBar) {
  }

  ngOnInit() {
    this.notificationService.getNotificationsObservable()
      .pipe(
        takeUntil(this.destroy$)
      )
      .subscribe(n => {
        if (n.type == NotificationType.ERROR) {
          return this.notifications.push(n);
        } else {
          this._snackBar.open(n.content, null, {
            duration: 3000,
          });
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next(true);
    this.destroy$.unsubscribe();
  }

}
