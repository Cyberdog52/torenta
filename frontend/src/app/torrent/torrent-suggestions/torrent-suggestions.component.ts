import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TorrentEntry } from '../../shared/dto/pirateBay/TorrentEntry';
import { TorrentService } from '../torrent.service';
import {
  DownloadRequestDto,
  getDownloadTitle,
  getEpisodeString,
} from '../../shared/dto/torrent/DownloadRequestDto';
import { TmdbEpisodeDto } from '../../shared/dto/tmdb/TmdbEpisodeDto';
import { TmdbSeriesDetailDto } from '../../shared/dto/tmdb/TmdbSeriesDetailDto';
import { TmdbMovieDetailDto } from '../../shared/dto/tmdb/TmdbMovieDetailDto';
import { NotificationService } from '../../shared/notification/notification.service';
import { NotificationType } from '../../shared/dto/notification/Notification';
import { safeValue } from '../../shared/resource';

@Component({
  selector: 'app-torrent-suggestions',
  imports: [MatTableModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './torrent-suggestions.component.scss',
  templateUrl: './torrent-suggestions.component.html',
})
export class TorrentSuggestionsComponent {
  private readonly torrentService = inject(TorrentService);
  private readonly notificationService = inject(NotificationService);

  readonly seriesDetail = input<TmdbSeriesDetailDto | null>(null);
  readonly tmdbEpisodeDto = input<TmdbEpisodeDto | null>(null);
  readonly movieDetail = input<TmdbMovieDetailDto | null>(null);
  readonly searchString = input<string | null>(null);

  protected readonly displayedColumns = [
    'name',
    'seeders',
    'time',
    'size',
    'trusted',
    'startDownload',
  ];

  /**
   * The effective query: either what the user typed, or one derived from the
   * selected episode / movie.
   */
  private readonly query = computed(() => {
    const explicit = this.searchString();
    if (explicit) {
      return explicit;
    }
    const seriesDetail = this.seriesDetail();
    if (seriesDetail) {
      const title = `${seriesDetail.name} ${getEpisodeString(this.tmdbEpisodeDto())}`;
      // 'Star Wars: Andor S01E01' -> 'Andor S01E01'
      return dropFranchisePrefix(title);
    }
    const movieTitle = this.movieDetail()?.title;
    return movieTitle != null ? dropFranchisePrefix(movieTitle) : undefined;
  });

  private readonly suggestionsResource = this.torrentService.torrentSearchResource(this.query);

  protected readonly suggestions = computed(() => safeValue(this.suggestionsResource) ?? []);
  protected readonly isLoading = this.suggestionsResource.isLoading;

  /**
   * Only polls while this component is on screen: `toSignal` subscribes here
   * and unsubscribes on destroy, which stops the shared refCounted poll.
   */
  private readonly downloads = toSignal(this.torrentService.downloads$, { initialValue: [] });

  protected hasStartedDownload(torrentEntry: TorrentEntry): boolean {
    return this.downloads().some(
      (download) => download.downloadRequest.torrentEntry.magnetLink === torrentEntry.magnetLink,
    );
  }

  protected startDownload(torrentEntry: TorrentEntry): void {
    const downloadRequest: DownloadRequestDto = {
      tmdbEpisode: this.tmdbEpisodeDto(),
      seriesDetail: this.seriesDetail(),
      torrentEntry,
      movieDetail: this.movieDetail(),
    };
    this.torrentService.startTorrent(downloadRequest).subscribe({
      next: () =>
        this.notificationService.notify({
          content: `Started downloading ${getDownloadTitle(downloadRequest)}`,
          type: NotificationType.INFO,
        }),
      error: () =>
        this.notificationService.notify({
          content: `Could not start download of ${getDownloadTitle(downloadRequest)}`,
          type: NotificationType.ERROR,
        }),
    });
  }
}

/**
 * Drops a leading franchise prefix, e.g. 'Star Wars: Andor S01E01' -> 'Andor S01E01'.
 * Only splits on the *first* colon and trims the result, so titles with more
 * than one colon (e.g. 'Marvel: Agents of S.H.I.E.L.D.: Season 1') keep
 * everything after the franchise name instead of losing it to `.pop()`.
 */
function dropFranchisePrefix(title: string): string {
  const separatorIndex = title.indexOf(':');
  return (separatorIndex === -1 ? title : title.slice(separatorIndex + 1)).trim();
}
