import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {DirectoryDto} from "../shared/dto/directory/DirectoryDto";

@Injectable({
  providedIn: 'root'
})
export class DirectoryService {

  private backendUrl = "api/directory";

  constructor(private httpClient: HttpClient) {
  }

  public getSeriesDirectory(seriesName: string): Observable<DirectoryDto> {
    let url = `${this.backendUrl}/series/${seriesName}`;
    return this.httpClient.get<DirectoryDto>(url);
  }
}
