import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
  WritableSignal,
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
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
import { ConciergeResultDto } from '../shared/dto/concierge/ConciergeResultDto';
import { catchError, map, of, Subject, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NotificationService } from '../shared/notification/notification.service';
import { NotificationType } from '../shared/dto/notification/Notification';

type MediaKind = 'series' | 'movie';

const TMDB_KEY_ERROR_MESSAGE = 'Set your TMDB service key in Preferences to start using Torenta.';

@Component({
  selector: 'app-search',
  imports: [
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatExpansionModule,
    MatChipsModule,
    MatTooltipModule,
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
  private readonly conciergeRequests = new Subject<string | null>();
  private readonly notificationService = inject(NotificationService);

  protected readonly conciergePrompt = signal('');
  protected readonly conciergeResults = signal<ConciergeResultDto[]>([]);
  protected readonly conciergeLoading = signal(false);
  protected readonly conciergeError = signal<string | null>(null);
  protected readonly conciergeSearched = signal(false);
  protected readonly seriesQuery = signal('');
  protected readonly movieQuery = signal('');
  protected readonly torrentQuery = signal('');
  protected readonly seriesInputValue = signal('');
  protected readonly movieInputValue = signal('');
  protected readonly torrentInputValue = signal('');

  protected readonly seriesSearch = this.searchService.searchSeriesResource(this.seriesQuery);
  protected readonly movieSearch = this.searchService.searchMoviesResource(this.movieQuery);

  constructor() {
    effect(() => {
      const err = this.seriesSearch.error();
      if (err instanceof HttpErrorResponse && err.status === 412) {
        this.notificationService.notify({
          content: TMDB_KEY_ERROR_MESSAGE,
          type: NotificationType.ERROR,
        });
      }
    });
    effect(() => {
      const err = this.movieSearch.error();
      if (err instanceof HttpErrorResponse && err.status === 412) {
        this.notificationService.notify({
          content: TMDB_KEY_ERROR_MESSAGE,
          type: NotificationType.ERROR,
        });
      }
    });
    // Lets other pages (e.g. Recommendations) deep-link into a series search,
    // for example `/search?series=The%20Office`.
    const seriesName = inject(ActivatedRoute).snapshot.queryParamMap.get('series');
    if (seriesName) {
      this.seriesInputValue.set(seriesName);
      this.seriesQuery.set(seriesName);
    }

    this.conciergeRequests
      .pipe(
        switchMap((prompt) => {
          if (prompt == null) {
            return of(null);
          }
          return this.searchService.searchConcierge({ prompt }).pipe(
            map((response) => ({ response, failed: false }) as const),
            catchError(() => of({ response: null, failed: true } as const)),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((outcome) => {
        if (outcome == null) {
          return;
        }
        this.conciergeLoading.set(false);
        if (outcome.failed) {
          this.conciergeError.set('We could not complete your concierge search. Please try again.');
          return;
        }
        this.conciergeResults.set(outcome.response.results.toSorted((a, b) => a.rank - b.rank));
      });
  }

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

  protected submitConcierge(promptValue: string): void {
    const prompt = promptValue.trim();
    this.conciergePrompt.set(promptValue);
    this.conciergeRequests.next(null);
    this.conciergeResults.set([]);
    this.conciergeError.set(null);
    this.conciergeSearched.set(true);
    this.openPanels.set(new Set());

    if (!prompt) {
      this.conciergeLoading.set(false);
      this.conciergeError.set('Please describe what you would like to watch.');
      return;
    }

    this.conciergeLoading.set(true);
    this.conciergeRequests.next(prompt);
  }

  protected conciergePanelKey(result: ConciergeResultDto): string {
    return `concierge:${result.mediaType}:${result.id}`;
  }

  protected setConciergePanelOpen(key: string, open: boolean): void {
    this.updateOpenPanel(key, open);
  }

  protected isConciergePanelOpen(key: string): boolean {
    return this.openPanels().has(key);
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
    this.updateOpenPanel(key, open);
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
    return `search:${kind}:${id}`;
  }

  private updateOpenPanel(key: string, open: boolean): void {
    this.openPanels.update((keys) => {
      const next = new Set(keys);
      if (open) {
        next.add(key);
      } else {
        next.delete(key);
      }
      return next;
    });
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
