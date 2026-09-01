import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { TmdbEpisodeDto } from '../../shared/dto/tmdb/TmdbEpisodeDto';
import { TmdbSeriesDetailDto } from '../../shared/dto/tmdb/TmdbSeriesDetailDto';
import { DownloadStatus } from '../../shared/dto/library/DownloadStatus';
import { TorrentSuggestionsComponent } from '../../torrent/torrent-suggestions/torrent-suggestions.component';
import { OverviewPopoverComponent } from '../overview-popover/overview-popover.component';

@Component({
  selector: 'app-episode',
  imports: [MatIconModule, TorrentSuggestionsComponent, OverviewPopoverComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './episode.component.scss',
  templateUrl: './episode.component.html',
})
export class EpisodeComponent {
  readonly tmdbEpisodeDto = input.required<TmdbEpisodeDto>();
  readonly seriesDetail = input.required<TmdbSeriesDetailDto>();
  readonly downloadStatus = input<DownloadStatus | null>(null);

  protected readonly downloadText = computed(() => {
    switch (this.downloadStatus()) {
      case DownloadStatus.DOWNLOADING:
        return 'Downloading';
      case DownloadStatus.DOWNLOADED:
        return 'Downloaded';
      default:
        return null;
    }
  });
}
