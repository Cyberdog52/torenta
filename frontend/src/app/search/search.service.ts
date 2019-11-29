import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {SeriesDetail} from "../shared/dto/SeriesDetail";

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
    let url = `${this.backendUrl}/detail?id=${id}`;
    return this.httpClient.get<SeriesDetail>(url);
  }


}
