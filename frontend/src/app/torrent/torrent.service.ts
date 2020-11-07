import {Injectable, OnDestroy} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {BehaviorSubject, Observable} from "rxjs";
import {DownloadDto} from "../shared/dto/torrent/DownloadDto";
import {DownloadRequest} from "../shared/dto/torrent/DownloadRequest";
import {TorrentEntry} from "../shared/dto/pirateBay/TorrentEntry";

@Injectable({
  providedIn: 'root'
})
export class TorrentService implements OnDestroy{

  private backendUrl = "api/bittorrent";

  private _downloadDtos = new BehaviorSubject<DownloadDto[]>([]);
  private intervalId: any;
  private updateIntervalInMs = 1000;

  constructor(private httpClient: HttpClient) {
    this.intervalId = setInterval(() => this.updateDownloadDtos(), this.updateIntervalInMs);
  }

  ngOnDestroy(): void {
    clearInterval(this.intervalId);
  }

  public getDownloadDtosObservable(): Observable<DownloadDto[]>  {
    return this._downloadDtos.asObservable();
  }

  public startTorrent(downloadRequest: DownloadRequest): Observable<any> {
    let url = `${this.backendUrl}`;
    return this.httpClient.post<any>(url, downloadRequest);
  }

  private getDownloadDtos(): Observable<DownloadDto[]> {
    let url = `${this.backendUrl}`;
    return this.httpClient.get<DownloadDto[]>(url);
  }

  private updateDownloadDtos(): void {
    this.getDownloadDtos().subscribe(downloadDtos => {
      const sortedDownloadDtos = downloadDtos.sort((a, b) => b.startTimeInMs - a.startTimeInMs);
      this._downloadDtos.next(sortedDownloadDtos);
    });
  }

  public searchTorrent(searchString: string): Observable<TorrentEntry[]> {
    let url = `api/torrent/?search=${searchString}`;
    return this.httpClient.get<TorrentEntry[]>(url);
  }
}
