import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {Series} from "../shared/dto/library/Series";

@Injectable({
  providedIn: 'root'
})
export class LibraryService {

  private backendUrl = "api/library";

  constructor(private httpClient: HttpClient) {
  }

  public getSeriesInLibrary(name: string): Observable<Series> {
    let url = `${this.backendUrl}/tv/${name}`;
    return this.httpClient.get<Series>(url);
  }
}
