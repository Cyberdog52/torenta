import { Injectable, Signal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { TmdbSeriesDetailDto } from '../shared/dto/tmdb/TmdbSeriesDetailDto';
import { TmdbEpisodeDto } from '../shared/dto/tmdb/TmdbEpisodeDto';
import { TmdbMovieDetailDto } from '../shared/dto/tmdb/TmdbMovieDetailDto';
import { TmdbSearchSeriesResultDto } from '../shared/dto/tmdb/TmdbSearchSeriesResultDto';
import { TmdbSearchMoviesResultDto } from '../shared/dto/tmdb/TmdbSearchMoviesResultDto';

const BACKEND_URL = 'api/tmdb';

@Injectable({ providedIn: 'root' })
export class SearchService {
  /** Reactive TV show search. Stays idle while the query is empty. */
  searchSeriesResource(query: Signal<string>) {
    return httpResource<TmdbSearchSeriesResultDto>(() => {
      const search = query().trim();
      return search ? { url: `${BACKEND_URL}/tv`, params: { search } } : undefined;
    });
  }

  /** Reactive movie search. Stays idle while the query is empty. */
  searchMoviesResource(query: Signal<string>) {
    return httpResource<TmdbSearchMoviesResultDto>(() => {
      const search = query().trim();
      return search ? { url: `${BACKEND_URL}/movie`, params: { search } } : undefined;
    });
  }

  seriesDetailResource(id: Signal<number>) {
    return httpResource<TmdbSeriesDetailDto>(() => `${BACKEND_URL}/tv/${id()}`);
  }

  movieDetailResource(id: Signal<number>) {
    return httpResource<TmdbMovieDetailDto>(() => `${BACKEND_URL}/movie/${id()}`);
  }

  episodesResource(seriesId: Signal<number>, seasonNumber: Signal<number>) {
    return httpResource<TmdbEpisodeDto[]>(
      () => `${BACKEND_URL}/tv/${seriesId()}/season/${seasonNumber()}`,
    );
  }
}
