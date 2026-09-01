import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  output,
} from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { SearchService } from '../../search/search.service';
import { DirectoryService } from '../../directory/directory.service';
import { TorrentSuggestionsComponent } from '../../torrent/torrent-suggestions/torrent-suggestions.component';
import { safeValue } from '../../shared/resource';
import { TmdbMovieDetailDto } from '../../shared/dto/tmdb/TmdbMovieDetailDto';
import { DirectoryDto } from '../../shared/dto/directory/DirectoryDto';

@Component({
  selector: 'app-movie-detail',
  imports: [MatProgressSpinnerModule, TorrentSuggestionsComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './movie-detail.component.scss',
  templateUrl: './movie-detail.component.html',
})
export class MovieDetailComponent {
  private readonly searchService = inject(SearchService);
  private readonly directoryService = inject(DirectoryService);

  readonly id = input.required<number>();
  readonly detailLoaded = output<TmdbMovieDetailDto>();
  readonly directoryLoaded = output<DirectoryDto>();

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

  constructor() {
    effect(() => {
      const detail = this.movieDetail();
      if (detail != null) {
        this.detailLoaded.emit(detail);
      }
    });
    effect(() => {
      const directory = this.movieDirectory();
      if (directory != null) {
        this.directoryLoaded.emit(directory);
      }
    });
  }
}
