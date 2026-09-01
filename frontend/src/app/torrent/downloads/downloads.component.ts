import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { TorrentService } from '../torrent.service';
import { DownloadDto } from '../../shared/dto/torrent/DownloadDto';
import { DownloadState } from '../../shared/dto/torrent/DownloadState';
import { NotificationService } from '../../shared/notification/notification.service';
import { NotificationType } from '../../shared/dto/notification/Notification';
import { getDownloadTitle } from '../../shared/dto/torrent/DownloadRequestDto';
import {
  DownloadAction,
  DownloadDetailComponent,
} from './download-detail/download-detail.component';
import { MatDialog } from '@angular/material/dialog';
import { finalize, Observable } from 'rxjs';
import { ConfirmStopDialogComponent } from './confirm-stop-dialog.component';

@Component({
  selector: 'app-downloads',
  imports: [MatCardModule, MatIconModule, DownloadDetailComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './downloads.component.scss',
  templateUrl: './downloads.component.html',
})
export class DownloadsComponent {
  private readonly torrentService = inject(TorrentService);
  private readonly notificationService = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  protected readonly pendingActions = signal<ReadonlySet<string>>(new Set());

  /**
   * Only polls while this component is on screen: `toSignal` subscribes here
   * and unsubscribes on destroy, which stops the shared refCounted poll.
   */
  protected readonly downloads = toSignal(this.torrentService.downloads$, { initialValue: [] });

  private previousDownloads: DownloadDto[] = [];

  constructor() {
    this.torrentService.downloads$.pipe(takeUntilDestroyed()).subscribe((downloads) => {
      this.notifyNewlyFinished(this.previousDownloads, downloads);
      this.previousDownloads = downloads;
    });
  }

  protected requestAction(download: DownloadDto, action: DownloadAction): void {
    if (this.pendingActions().has(download.id)) {
      return;
    }
    if (action === 'stopAndDelete') {
      this.dialog
        .open(ConfirmStopDialogComponent, { data: this.titleOf(download) })
        .afterClosed()
        .subscribe((confirmed) => {
          if (confirmed) {
            this.runAction(download, action);
          }
        });
      return;
    }
    this.runAction(download, action);
  }

  private runAction(download: DownloadDto, action: DownloadAction): void {
    this.pendingActions.update((ids) => new Set(ids).add(download.id));
    this.actionRequest(download.id, action)
      .pipe(
        finalize(() =>
          this.pendingActions.update((ids) => {
            const next = new Set(ids);
            next.delete(download.id);
            return next;
          }),
        ),
      )
      .subscribe({
        next: () =>
          this.notificationService.notify({
            type: NotificationType.INFO,
            content: this.successMessage(download, action),
          }),
        error: () =>
          this.notificationService.notify({
            type: NotificationType.ERROR,
            content: `Could not ${this.actionLabel(action)} ${this.titleOf(download)}.`,
          }),
      });
  }

  private actionRequest(id: string, action: DownloadAction): Observable<void> {
    switch (action) {
      case 'pause':
        return this.torrentService.pauseTorrent(id);
      case 'restart':
        return this.torrentService.restartTorrent(id);
      case 'stopAndDelete':
        return this.torrentService.stopAndDeleteTorrent(id);
      case 'remove':
        return this.torrentService.removeTorrentTile(id);
    }
  }

  private titleOf(download: DownloadDto): string {
    return (
      download.displayTitle ??
      (download.downloadRequest == null
        ? `download ${download.id}`
        : getDownloadTitle(download.downloadRequest))
    );
  }

  private successMessage(download: DownloadDto, action: DownloadAction): string {
    const labels: Record<DownloadAction, string> = {
      pause: 'paused',
      restart: 'restarted',
      stopAndDelete: 'stopped and deleted',
      remove: 'removed from Downloads',
    };
    return `${this.titleOf(download)} ${labels[action]}.`;
  }

  private actionLabel(action: DownloadAction): string {
    return action === 'stopAndDelete' ? 'stop and delete' : action;
  }

  private notifyNewlyFinished(previous: DownloadDto[], current: DownloadDto[]): void {
    for (const download of current) {
      if (download.state !== DownloadState.FINISHED) {
        continue;
      }
      const before = previous.find((existing) => existing.id === download.id);
      if (before != null && before.state !== download.state) {
        this.notificationService.notify({
          content: `${this.titleOf(download)} successfully downloaded.`,
          type: NotificationType.INFO,
        });
      }
    }
  }
}
