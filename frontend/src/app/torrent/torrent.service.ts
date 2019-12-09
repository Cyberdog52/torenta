import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {DownloadDto} from "../shared/dto/torrent/DownloadDto";

@Injectable({
  providedIn: 'root'
})
export class TorrentService {

  private backendUrl = "api/bittorrent";

  constructor(private httpClient: HttpClient) {
  }

  public startTorrent(magnetLink: string): Observable<any> {
    let url = `${this.backendUrl}`;
    return this.httpClient.post<any>(url, magnetLink);
  }

  public getIds(): Observable<number[]> {
    let url = `${this.backendUrl}`;
    return this.httpClient.get<number[]>(url);
  }

  public getDownloadDto(id: number): Observable<DownloadDto> {
    let url = `${this.backendUrl}/${id}`;
    return this.httpClient.get<DownloadDto>(url);
  }
}
