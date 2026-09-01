import {
  ChangeDetectionStrategy,
  Component,
  WritableSignal,
  computed,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';
import { SearchService } from './search.service';
import { DelayedKeyupDirective } from '../shared/delayed-keyup.directive';
import { SeriesDetailComponent } from '../tmdb/series-detail/series-detail.component';
import { MovieDetailComponent } from '../tmdb/movie-detail/movie-detail.component';
import { TorrentSuggestionsComponent } from '../torrent/torrent-suggestions/torrent-suggestions.component';
import { backdropUrl, posterUrl } from '../shared/tmdb-images';
import { safeValue } from '../shared/resource';
import { TmdbSeriesDetailDto } from '../shared/dto/tmdb/TmdbSeriesDetailDto';
import { TmdbMovieDetailDto } from '../shared/dto/tmdb/TmdbMovieDetailDto';
import { DirectoryDto } from '../shared/dto/directory/DirectoryDto';
import { OverviewPopoverComponent } from '../tmdb/overview-popover/overview-popover.component';

type MediaKind = 'series' | 'movie';

@Component({
  selector: 'app-search',
  imports: [
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatExpansionModule,
    MatChipsModule,
    MatTooltipModule,
    MatButtonModule,
    DelayedKeyupDirective,
    SeriesDetailComponent,
    MovieDetailComponent,
    TorrentSuggestionsComponent,
    OverviewPopoverComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './search.component.scss',
  templateUrl: './search.component.html',
})
export class SearchComponent {
  private readonly searchService = inject(SearchService);

  protected readonly seriesQuery = signal('');
  protected readonly movieQuery = signal('');
  protected readonly torrentQuery = signal('');
  protected readonly seriesInputValue = signal('');
  protected readonly movieInputValue = signal('');
  protected readonly torrentInputValue = signal('');

  protected readonly seriesSearch = this.searchService.searchSeriesResource(this.seriesQuery);
  protected readonly movieSearch = this.searchService.searchMoviesResource(this.movieQuery);

  /**
   * Sorted copies, computed with `toSorted` so the underlying resource value
   * is never mutated by re-running this on every change-detection pass.
   */
  protected readonly seriesOverviews = computed(() => {
    if (!this.seriesQuery().trim()) {
      return [];
    }
    return (
      safeValue(this.seriesSearch)?.results.toSorted((a, b) => b.popularity - a.popularity) ?? []
    );
  });

  protected readonly movieOverviews = computed(() => {
    if (!this.movieQuery().trim()) {
      return [];
    }
    return (
      safeValue(this.movieSearch)?.results.toSorted((a, b) => b.popularity - a.popularity) ?? []
    );
  });

  /**
   * Ids of the expanded panels. Detail components are only rendered for open
   * panels, otherwise we would load every series detail at once and blow past
   * TMDB's rate limit of 40 requests / 10s.
   */
  private readonly openPanels = signal<ReadonlySet<string>>(new Set());
  protected readonly seriesDetails = signal<ReadonlyMap<number, TmdbSeriesDetailDto>>(new Map());
  protected readonly movieDetails = signal<ReadonlyMap<number, TmdbMovieDetailDto>>(new Map());
  protected readonly movieDirectories = signal<ReadonlyMap<number, DirectoryDto>>(new Map());

  protected readonly posterUrl = posterUrl;
  protected readonly backdropUrl = backdropUrl;

  constructor() {
    // Lets other pages (e.g. Recommendations) deep-link into a series search,
    // for example `/search?series=The%20Office`.
    const seriesName = inject(ActivatedRoute).snapshot.queryParamMap.get('series');
    if (seriesName) {
      this.seriesInputValue.set(seriesName);
      this.seriesQuery.set(seriesName);
    }
  }

  protected updateInputValue(value: WritableSignal<string>, input: HTMLInputElement): void {
    value.set(input.value);
  }

  protected clearSearch(
    value: WritableSignal<string>,
    query: WritableSignal<string>,
    input: HTMLInputElement,
  ): void {
    input.value = '';
    value.set('');
    query.set('');
    input.focus();
  }

  protected setPanelOpen(kind: MediaKind, id: number, open: boolean): void {
    const key = this.panelKey(kind, id);
    this.openPanels.update((ids) => {
      const next = new Set(ids);
      if (open) {
        next.add(key);
      } else {
        next.delete(key);
      }
      return next;
    });
    if (!open) {
      if (kind === 'series') {
        this.deleteMapEntry(this.seriesDetails, id);
      } else {
        this.deleteMapEntry(this.movieDetails, id);
        this.deleteMapEntry(this.movieDirectories, id);
      }
    }
  }

  protected isPanelOpen(kind: MediaKind, id: number): boolean {
    return this.openPanels().has(this.panelKey(kind, id));
  }

  protected isPanelBackdropVisible(kind: MediaKind, id: number): boolean {
    return this.isPanelOpen(kind, id);
  }

  protected setSeriesDetail(detail: TmdbSeriesDetailDto): void {
    this.setMapEntry(this.seriesDetails, detail.id, detail);
  }

  protected setMovieDetail(detail: TmdbMovieDetailDto): void {
    this.setMapEntry(this.movieDetails, detail.id, detail);
  }

  protected setMovieDirectory(movieId: number, directory: DirectoryDto): void {
    this.setMapEntry(this.movieDirectories, movieId, directory);
  }

  protected seriesRuntime(detail: TmdbSeriesDetailDto): number | null {
    return detail.episode_run_time[0] ?? null;
  }

  private panelKey(kind: MediaKind, id: number): string {
    return `${kind}:${id}`;
  }

  private setMapEntry<T>(
    target: WritableSignal<ReadonlyMap<number, T>>,
    id: number,
    value: T,
  ): void {
    target.update((entries) => {
      const next = new Map(entries);
      next.set(id, value);
      return next;
    });
  }

  private deleteMapEntry<T>(target: WritableSignal<ReadonlyMap<number, T>>, id: number): void {
    target.update((entries) => {
      const next = new Map(entries);
      next.delete(id);
      return next;
    });
  }
}
