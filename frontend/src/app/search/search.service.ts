import { inject, Injectable, Signal } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { Observable, timeout } from 'rxjs';
import { TmdbSeriesDetailDto } from '../shared/dto/tmdb/TmdbSeriesDetailDto';
import { TmdbEpisodeDto } from '../shared/dto/tmdb/TmdbEpisodeDto';
import { TmdbMovieDetailDto } from '../shared/dto/tmdb/TmdbMovieDetailDto';
import { TmdbSearchSeriesResultDto } from '../shared/dto/tmdb/TmdbSearchSeriesResultDto';
import { TmdbSearchMoviesResultDto } from '../shared/dto/tmdb/TmdbSearchMoviesResultDto';
import { ConciergeSearchRequestDto } from '../shared/dto/concierge/ConciergeSearchRequestDto';
import { ConciergeSearchResponseDto } from '../shared/dto/concierge/ConciergeSearchResponseDto';

const BACKEND_URL = 'api/tmdb';
const CONCIERGE_URL = 'api/concierge/search';
const CONCIERGE_TIMEOUT_MS = 130_000;

@Injectable({ providedIn: 'root' })
export class SearchService {
  private readonly httpClient = inject(HttpClient);

  searchConcierge(request: ConciergeSearchRequestDto): Observable<ConciergeSearchResponseDto> {
    return this.httpClient
      .post<ConciergeSearchResponseDto>(CONCIERGE_URL, request)
      .pipe(timeout(CONCIERGE_TIMEOUT_MS));
  }

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
