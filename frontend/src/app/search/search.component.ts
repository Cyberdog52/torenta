import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatExpansionModule } from '@angular/material/expansion';
import { SearchService } from './search.service';
import { DelayedKeyupDirective } from '../shared/delayed-keyup.directive';
import { SeriesDetailComponent } from '../tmdb/series-detail/series-detail.component';
import { MovieDetailComponent } from '../tmdb/movie-detail/movie-detail.component';
import { TorrentSuggestionsComponent } from '../torrent/torrent-suggestions/torrent-suggestions.component';
import { backdropUrl, posterUrl } from '../shared/tmdb-images';
import { safeValue } from '../shared/resource';
import { NotificationService } from '../shared/notification/notification.service';
import { NotificationType } from '../shared/dto/notification/Notification';

const TMDB_KEY_ERROR_MESSAGE = 'Set your TMDB service key in Preferences to start using Torenta.';

@Component({
  selector: 'app-search',
  imports: [
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatExpansionModule,
    DelayedKeyupDirective,
    SeriesDetailComponent,
    MovieDetailComponent,
    TorrentSuggestionsComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './search.component.scss',
  templateUrl: './search.component.html',
})
export class SearchComponent {
  private readonly searchService = inject(SearchService);
  private readonly notificationService = inject(NotificationService);

  protected readonly seriesQuery = signal('');
  protected readonly movieQuery = signal('');
  protected readonly torrentQuery = signal('');

  protected readonly seriesSearch = this.searchService.searchSeriesResource(this.seriesQuery);
  protected readonly movieSearch = this.searchService.searchMoviesResource(this.movieQuery);

  constructor() {
    effect(() => {
      const err = this.seriesSearch.error();
      if (err instanceof HttpErrorResponse && err.status === 412) {
        this.notificationService.notify({ content: TMDB_KEY_ERROR_MESSAGE, type: NotificationType.ERROR });
      }
    });
    effect(() => {
      const err = this.movieSearch.error();
      if (err instanceof HttpErrorResponse && err.status === 412) {
        this.notificationService.notify({ content: TMDB_KEY_ERROR_MESSAGE, type: NotificationType.ERROR });
      }
    });
  }

  /**
   * Sorted copies, computed with `toSorted` so the underlying resource value
   * is never mutated by re-running this on every change-detection pass.
   */
  protected readonly seriesOverviews = computed(
    () =>
      safeValue(this.seriesSearch)?.results.toSorted((a, b) => b.popularity - a.popularity) ?? [],
  );

  protected readonly movieOverviews = computed(
    () =>
      safeValue(this.movieSearch)?.results.toSorted((a, b) => b.popularity - a.popularity) ?? [],
  );

  /**
   * Ids of the expanded panels. Detail components are only rendered for open
   * panels, otherwise we would load every series detail at once and blow past
   * TMDB's rate limit of 40 requests / 10s.
   */
  private readonly openPanels = signal<ReadonlySet<number>>(new Set());
  private readonly focusedPanel = signal<number | null>(null);

  protected readonly posterUrl = posterUrl;
  protected readonly backdropUrl = backdropUrl;

  protected setPanelOpen(id: number, open: boolean): void {
    this.openPanels.update((ids) => {
      const next = new Set(ids);
      if (open) {
        next.add(id);
      } else {
        next.delete(id);
      }
      return next;
    });
  }

  protected isPanelOpen(id: number): boolean {
    return this.openPanels().has(id);
  }

  protected setPanelFocused(id: number, focused: boolean): void {
    this.focusedPanel.update((current) => {
      if (focused) {
        return id;
      }
      return current === id ? null : current;
    });
  }

  protected isPanelBackdropVisible(id: number): boolean {
    return this.isPanelOpen(id) || this.focusedPanel() === id;
  }
}
