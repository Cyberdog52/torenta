import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { SearchService } from '../../search/search.service';
import { DirectoryService } from '../../directory/directory.service';
import { TorrentSuggestionsComponent } from '../../torrent/torrent-suggestions/torrent-suggestions.component';
import { safeValue } from '../../shared/resource';

@Component({
  selector: 'app-movie-detail',
  imports: [MatIconModule, MatChipsModule, MatProgressSpinnerModule, TorrentSuggestionsComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './movie-detail.component.scss',
  templateUrl: './movie-detail.component.html',
})
export class MovieDetailComponent {
  private readonly searchService = inject(SearchService);
  private readonly directoryService = inject(DirectoryService);

  readonly id = input.required<number>();

  private readonly movie = this.searchService.movieDetailResource(this.id);

  protected readonly movieDetail = this.movie.value;
  protected readonly isLoading = this.movie.isLoading;

  private readonly directory = this.directoryService.movieDirectoryResource(
    computed(() => this.movieDetail()?.title.replace(/[^a-zA-Z0-9.\- ]/, '')),
    computed(() => {
      const releaseDate = this.movieDetail()?.release_date;
      return releaseDate ? Number(releaseDate.split('-')[0]) : undefined;
    }),
  );

  protected readonly movieDirectory = computed(() => safeValue(this.directory));
}
