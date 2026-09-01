import { inject, Injectable, Signal } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { catchError, map, Observable, of, shareReplay, switchMap, timer } from 'rxjs';
import { DownloadDto } from '../shared/dto/torrent/DownloadDto';
import { DownloadRequestDto } from '../shared/dto/torrent/DownloadRequestDto';
import { TorrentEntry } from '../shared/dto/pirateBay/TorrentEntry';
import { NotificationService } from '../shared/notification/notification.service';
import { NotificationType } from '../shared/dto/notification/Notification';

const BACKEND_URL = 'api/bittorrent';
const POLL_INTERVAL_MS = 1000;

@Injectable({ providedIn: 'root' })
export class TorrentService {
  private readonly httpClient = inject(HttpClient);
  private readonly notificationService = inject(NotificationService);

  /**
   * Polls the backend for the current downloads, newest first.
   *
   * Polling starts on first subscription and stops again once the last
   * subscriber goes away, so navigating away actually stops the traffic.
   */
  readonly downloads$: Observable<DownloadDto[]> = timer(0, POLL_INTERVAL_MS).pipe(
    switchMap(() =>
      this.httpClient.get<DownloadDto[]>(BACKEND_URL).pipe(
        catchError(() => {
          // A failed poll must not look like "no downloads": tell the user
          // instead of silently reporting an empty list.
          this.notificationService.notify({
            type: NotificationType.ERROR,
            content: 'Lost connection to the backend. Retrying…',
          });
          return of([]);
        }),
      ),
    ),
    map((downloads) => downloads.toSorted((a, b) => b.startTimeInMs - a.startTimeInMs)),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  startTorrent(downloadRequest: DownloadRequestDto): Observable<void> {
    return this.httpClient.post<void>(BACKEND_URL, downloadRequest);
  }

  pauseTorrent(id: string): Observable<void> {
    return this.httpClient.post<void>(`${BACKEND_URL}/${id}/pause`, null);
  }

  restartTorrent(id: string): Observable<void> {
    return this.httpClient.post<void>(`${BACKEND_URL}/${id}/restart`, null);
  }

  stopAndDeleteTorrent(id: string): Observable<void> {
    return this.httpClient.delete<void>(`${BACKEND_URL}/${id}`);
  }

  removeTorrentTile(id: string): Observable<void> {
    return this.httpClient.delete<void>(`${BACKEND_URL}/${id}/tile`);
  }

  torrentSearchResource(searchString: Signal<string | undefined>) {
    return httpResource<TorrentEntry[]>(() => {
      const search = searchString()?.trim();
      return search ? { url: 'api/torrent', params: { search } } : undefined;
    });
  }
}
