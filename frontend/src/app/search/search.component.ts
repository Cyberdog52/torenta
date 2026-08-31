import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
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
import { posterUrl } from '../shared/tmdb-images';
import { safeValue } from '../shared/resource';

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

  protected readonly seriesQuery = signal('');
  protected readonly movieQuery = signal('');
  protected readonly torrentQuery = signal('');

  protected readonly seriesSearch = this.searchService.searchSeriesResource(this.seriesQuery);
  protected readonly movieSearch = this.searchService.searchMoviesResource(this.movieQuery);

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

  protected readonly posterUrl = posterUrl;

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
}
