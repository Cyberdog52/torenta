import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  linkedSignal,
} from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { SearchService } from '../../search/search.service';
import { LibraryService } from '../../library/library.service';
import { TmdbSeasonDto } from '../../shared/dto/tmdb/TmdbSeasonDto';
import { TmdbEpisodeDto } from '../../shared/dto/tmdb/TmdbEpisodeDto';
import { TmdbSeriesDetailDto } from '../../shared/dto/tmdb/TmdbSeriesDetailDto';
import { DownloadStatus } from '../../shared/dto/library/DownloadStatus';
import { EpisodeComponent } from '../episode/episode.component';
import { safeValue } from '../../shared/resource';

@Component({
  selector: 'app-season',
  imports: [MatIconModule, MatButtonModule, EpisodeComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './season.component.scss',
  templateUrl: './season.component.html',
})
export class SeasonComponent {
  private readonly searchService = inject(SearchService);
  private readonly libraryService = inject(LibraryService);

  readonly tmdbSeasonDto = input.required<TmdbSeasonDto>();
  readonly seriesDetail = input.required<TmdbSeriesDetailDto>();

  protected readonly showEpisode = linkedSignal<TmdbSeasonDto, TmdbEpisodeDto | null>({
    source: this.tmdbSeasonDto,
    computation: () => null,
  });

  /**
   * A season does not ship its episodes, so they are fetched separately.
   * The resource re-requests automatically when the selected season changes.
   */
  private readonly episodesResource = this.searchService.episodesResource(
    computed(() => this.seriesDetail().id),
    computed(() => this.tmdbSeasonDto().season_number),
  );

  protected readonly episodes = computed(() => safeValue(this.episodesResource) ?? []);

  private readonly series = this.libraryService.seriesInLibraryResource(
    computed(() => this.seriesDetail().name),
  );

  protected episodeButtonTitle(episode: TmdbEpisodeDto): string {
    return `E${String(episode.episode_number).padStart(2, '0')}`;
  }

  protected downloadStatus(tmdbEpisodeDto: TmdbEpisodeDto | null): DownloadStatus | null {
    if (tmdbEpisodeDto == null) {
      return null;
    }
    const season = safeValue(this.series)?.seasonList.find(
      (s) => s.seasonNumber === tmdbEpisodeDto.season_number,
    );
    const episode = season?.episodeList.find(
      (e) => e.episodeNumber === tmdbEpisodeDto.episode_number,
    );
    return episode?.downloadStatus ?? null;
  }

  protected episodeIcon(tmdbEpisodeDto: TmdbEpisodeDto): string | null {
    if (SeasonComponent.notAiredYet(tmdbEpisodeDto)) {
      return 'date_range';
    }
    switch (this.downloadStatus(tmdbEpisodeDto)) {
      case DownloadStatus.DOWNLOADED:
        return 'done';
      case DownloadStatus.DOWNLOADING:
        return 'arrow_downward';
      default:
        return null;
    }
  }

  /** Text equivalent for `episodeIcon`, so status isn't conveyed by icon shape alone. */
  protected episodeIconLabel(tmdbEpisodeDto: TmdbEpisodeDto): string | null {
    if (SeasonComponent.notAiredYet(tmdbEpisodeDto)) {
      return 'Not yet aired';
    }
    switch (this.downloadStatus(tmdbEpisodeDto)) {
      case DownloadStatus.DOWNLOADED:
        return 'Downloaded';
      case DownloadStatus.DOWNLOADING:
        return 'Downloading';
      default:
        return null;
    }
  }

  protected isSelected(tmdbEpisodeDto: TmdbEpisodeDto): boolean {
    return this.showEpisode()?.id === tmdbEpisodeDto.id;
  }

  private static notAiredYet(episode: TmdbEpisodeDto): boolean {
    if (episode.air_date == null) {
      return true;
    }
    return new Date(episode.air_date).valueOf() > Date.now();
  }
}
