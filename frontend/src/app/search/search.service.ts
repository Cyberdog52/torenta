import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {SeriesDetail} from "../shared/dto/tmdb/SeriesDetail";
import {Episode} from "../shared/dto/tmdb/Episode";

@Injectable({
  providedIn: 'root'
})
export class SearchService {

  private backendUrl = "api/tmdb/tv";

  constructor(private httpClient: HttpClient) {
  }

  public searchTmdb(searchString: string): Observable<SearchResult> {
    let url = `${this.backendUrl}?search=${searchString}`;
    return this.httpClient.get<SearchResult>(url);
  }

  public getTVShow(id: number): Observable<SeriesDetail> {
    let url = `${this.backendUrl}/${id}`;
    return this.httpClient.get<SeriesDetail>(url);
  }


  getEpisodes(seriesId: number, season_number: number): Observable<Episode[]> {
    let url = `${this.backendUrl}/${seriesId}/season/${season_number}`;
    return this.httpClient.get<Episode[]>(url);
  }
}
