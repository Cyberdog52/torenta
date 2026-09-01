import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TorrentService } from '../../torrent/torrent.service';
import { TorrentEntry } from '../../shared/dto/pirateBay/TorrentEntry';
import { SeriesRecommendation } from '../../shared/dto/recommendation/SeriesRecommendation';
import { RecommendedEpisode } from '../../shared/dto/recommendation/RecommendedEpisode';
import {
  DownloadRequestDto,
  dropFranchisePrefix,
  getDownloadTitle,
  getEpisodeString,
} from '../../shared/dto/torrent/DownloadRequestDto';
import { NotificationService } from '../../shared/notification/notification.service';
import { NotificationType } from '../../shared/dto/notification/Notification';
import { safeValue } from '../../shared/resource';

const TOP_TORRENT_COUNT = 3;

@Component({
  selector: 'app-series-torrents',
  imports: [MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './series-torrents.component.scss',
  templateUrl: './series-torrents.component.html',
})
export class SeriesTorrentsComponent {
  private readonly torrentService = inject(TorrentService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  readonly seriesDetail = input.required<SeriesRecommendation['seriesDetail']>();
  readonly episode = input.required<RecommendedEpisode>();

  /** Same query convention as the Search page: "<series name> S0xE0y", franchise prefix dropped. */
  private readonly query = computed(() =>
    dropFranchisePrefix(
      `${this.seriesDetail().name} ${getEpisodeString(this.episode().tmdbEpisodeDto)}`,
    ),
  );

  private readonly torrentsResource = this.torrentService.torrentSearchResource(this.query);

  protected readonly isLoading = this.torrentsResource.isLoading;

  /** Only trusted/VIP-uploaded torrents, best-seeded first, capped to the top few. */
  protected readonly topTorrents = computed(() =>
    (safeValue(this.torrentsResource) ?? [])
      .filter((torrent) => torrent.uploaderIsTrusted || torrent.uploaderIsVIP)
      .toSorted((a, b) => b.numberOfSeeders - a.numberOfSeeders)
      .slice(0, TOP_TORRENT_COUNT),
  );

  private readonly downloads = toSignal(this.torrentService.downloads$, { initialValue: [] });
  private readonly pendingDownloads = signal<ReadonlySet<string>>(new Set());

  protected hasStartedDownload(torrentEntry: TorrentEntry): boolean {
    return this.downloads().some(
      (download) => download.downloadRequest.torrentEntry.magnetLink === torrentEntry.magnetLink,
    );
  }

  protected isDownloadPending(torrentEntry: TorrentEntry): boolean {
    return this.pendingDownloads().has(torrentEntry.magnetLink);
  }

  protected startDownload(torrentEntry: TorrentEntry): void {
    if (this.hasStartedDownload(torrentEntry) || this.isDownloadPending(torrentEntry)) {
      return;
    }
    this.setDownloadPending(torrentEntry.magnetLink, true);

    const downloadRequest: DownloadRequestDto = {
      tmdbEpisode: this.episode().tmdbEpisodeDto,
      seriesDetail: this.seriesDetail(),
      torrentEntry,
      movieDetail: null,
    };
    this.torrentService.startTorrent(downloadRequest).subscribe({
      next: () =>
        this.notificationService.notify({
          content: `Started downloading ${getDownloadTitle(downloadRequest)}`,
          type: NotificationType.INFO,
          action: {
            label: 'Go to Downloads',
            onClick: () => void this.router.navigate(['/downloads']),
          },
        }),
      error: () => {
        this.setDownloadPending(torrentEntry.magnetLink, false);
        this.notificationService.notify({
          content: `Could not start download of ${getDownloadTitle(downloadRequest)}`,
          type: NotificationType.ERROR,
        });
      },
    });
  }

  private setDownloadPending(magnetLink: string, pending: boolean): void {
    this.pendingDownloads.update((entries) => {
      const next = new Set(entries);
      if (pending) {
        next.add(magnetLink);
      } else {
        next.delete(magnetLink);
      }
      return next;
    });
  }
}
