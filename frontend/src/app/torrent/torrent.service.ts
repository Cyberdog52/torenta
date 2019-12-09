import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {DownloadDto} from "../shared/dto/torrent/DownloadDto";
import {DownloadRequest} from "../shared/dto/torrent/DownloadRequest";

@Injectable({
  providedIn: 'root'
})
export class TorrentService {

  private backendUrl = "api/bittorrent";

  constructor(private httpClient: HttpClient) {
  }

  public startTorrent(downloadRequest: DownloadRequest): Observable<any> {
    let url = `${this.backendUrl}`;
    return this.httpClient.post<any>(url, downloadRequest);
  }

  public getDownloadDtos(): Observable<DownloadDto[]> {
    let url = `${this.backendUrl}`;
    return this.httpClient.get<DownloadDto[]>(url);
  }
}
