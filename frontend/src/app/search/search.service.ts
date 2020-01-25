import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {TmdbSeriesDetailDto} from "../shared/dto/tmdb/TmdbSeriesDetailDto";
import {TmdbEpisodeDto} from "../shared/dto/tmdb/TmdbEpisodeDto";

@Injectable({
  providedIn: 'root'
})
export class SearchService {

  private backendUrl = "api/tmdb/tv";

  constructor(private httpClient: HttpClient) {
  }

  public searchTmdb(searchString: string): Observable<TmdbSearchResultDto> {
    let url = `${this.backendUrl}?search=${searchString}`;
    return this.httpClient.get<TmdbSearchResultDto>(url);
  }

  public getTVShow(id: number): Observable<TmdbSeriesDetailDto> {
    let url = `${this.backendUrl}/${id}`;
    return this.httpClient.get<TmdbSeriesDetailDto>(url);
  }


  getEpisodes(seriesId: number, season_number: number): Observable<TmdbEpisodeDto[]> {
    let url = `${this.backendUrl}/${seriesId}/season/${season_number}`;
    return this.httpClient.get<TmdbEpisodeDto[]>(url);
  }
}
