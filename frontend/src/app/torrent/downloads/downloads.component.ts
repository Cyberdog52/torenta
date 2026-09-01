import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { TorrentService } from '../torrent.service';
import { DownloadDto } from '../../shared/dto/torrent/DownloadDto';
import { DownloadState } from '../../shared/dto/torrent/DownloadState';
import { NotificationService } from '../../shared/notification/notification.service';
import { NotificationType } from '../../shared/dto/notification/Notification';
import { getDownloadTitle } from '../../shared/dto/torrent/DownloadRequestDto';
import { DownloadDetailComponent } from './download-detail/download-detail.component';

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

  private notifyNewlyFinished(previous: DownloadDto[], current: DownloadDto[]): void {
    for (const download of current) {
      if (download.state !== DownloadState.FINISHED) {
        continue;
      }
      const before = previous.find((existing) => existing.id === download.id);
      if (before != null && before.state !== download.state) {
        this.notificationService.notify({
          content: `${getDownloadTitle(download.downloadRequest)} successfully downloaded.`,
          type: NotificationType.INFO,
        });
      }
    }
  }
}
