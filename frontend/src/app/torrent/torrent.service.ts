import { inject, Injectable, Signal } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { catchError, map, Observable, of, shareReplay, switchMap, timer } from 'rxjs';
import { DownloadDto } from '../shared/dto/torrent/DownloadDto';
import { DownloadRequestDto } from '../shared/dto/torrent/DownloadRequestDto';
import { TorrentEntry } from '../shared/dto/pirateBay/TorrentEntry';

const BACKEND_URL = 'api/bittorrent';
const POLL_INTERVAL_MS = 1000;

@Injectable({ providedIn: 'root' })
export class TorrentService {
  private readonly httpClient = inject(HttpClient);

  /**
   * Polls the backend for the current downloads, newest first.
   *
   * Polling starts on first subscription and stops again once the last
   * subscriber goes away, so navigating away actually stops the traffic.
   * The previous `setInterval` lived in a root provider whose `ngOnDestroy`
   * never fires, so it polled forever.
   */
  readonly downloads$: Observable<DownloadDto[]> = timer(0, POLL_INTERVAL_MS).pipe(
    switchMap(() => this.httpClient.get<DownloadDto[]>(BACKEND_URL).pipe(catchError(() => of([])))),
    map((downloads) => downloads.toSorted((a, b) => b.startTimeInMs - a.startTimeInMs)),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  startTorrent(downloadRequest: DownloadRequestDto): Observable<void> {
    return this.httpClient.post<void>(BACKEND_URL, downloadRequest);
  }

  torrentSearchResource(searchString: Signal<string | undefined>) {
    return httpResource<TorrentEntry[]>(() => {
      const search = searchString()?.trim();
      return search ? { url: 'api/torrent', params: { search } } : undefined;
    });
  }
}
