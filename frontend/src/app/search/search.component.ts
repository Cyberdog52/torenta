import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatInputModule } from '@angular/material/input';
import { MatExpansionModule } from '@angular/material/expansion';
import { SearchService } from './search.service';
import { DelayedKeyupDirective } from '../shared/delayed-keyup.directive';
import { SeriesDetailComponent } from '../tmdb/series-detail/series-detail.component';
import { MovieDetailComponent } from '../tmdb/movie-detail/movie-detail.component';
import { TorrentSuggestionsComponent } from '../torrent/torrent-suggestions/torrent-suggestions.component';
import { posterUrl } from '../shared/tmdb-images';

@Component({
  selector: 'app-search',
  imports: [
    MatCardModule,
    MatInputModule,
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

  private readonly seriesSearch = this.searchService.searchSeriesResource(this.seriesQuery);
  private readonly movieSearch = this.searchService.searchMoviesResource(this.movieQuery);

  /**
   * Sorted copies. The previous getters called `.sort()` on the response array
   * directly from the template, mutating it on every change-detection pass.
   */
  protected readonly seriesOverviews = computed(
    () => this.seriesSearch.value()?.results.toSorted((a, b) => b.popularity - a.popularity) ?? [],
  );

  protected readonly movieOverviews = computed(
    () => this.movieSearch.value()?.results.toSorted((a, b) => b.popularity - a.popularity) ?? [],
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
