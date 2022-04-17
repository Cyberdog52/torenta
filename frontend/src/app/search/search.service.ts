import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {TmdbSeriesDetailDto} from "../shared/dto/tmdb/TmdbSeriesDetailDto";
import {TmdbEpisodeDto} from "../shared/dto/tmdb/TmdbEpisodeDto";
import {TmdbMovieDetailDto} from "../shared/dto/tmdb/TmdbMovieDetailDto";

@Injectable({
  providedIn: 'root'
})
export class SearchService {

  private backendUrl = "api/tmdb";

  constructor(private httpClient: HttpClient) {
  }

  public searchSeries(searchString: string): Observable<TmdbSearchSeriesResultDto> {
    let url = `${this.backendUrl}/tv?search=${searchString}`;
    return this.httpClient.get<TmdbSearchSeriesResultDto>(url);
  }

  public searchMovies(searchString: string): Observable<TmdbSearchMoviesResultDto> {
    let url = `${this.backendUrl}/movie?search=${searchString}`;
    return this.httpClient.get<TmdbSearchMoviesResultDto>(url);
  }

  public getTVShow(id: number): Observable<TmdbSeriesDetailDto> {
    let url = `${this.backendUrl}/tv/${id}`;
    return this.httpClient.get<TmdbSeriesDetailDto>(url);
  }

  public getMovie(id: number): Observable<TmdbMovieDetailDto> {
    let url = `${this.backendUrl}/movie/${id}`;
    return this.httpClient.get<TmdbMovieDetailDto>(url);
  }


  getEpisodes(seriesId: number, season_number: number): Observable<TmdbEpisodeDto[]> {
    let url = `${this.backendUrl}/tv/${seriesId}/season/${season_number}`;
    return this.httpClient.get<TmdbEpisodeDto[]>(url);
  }
}
