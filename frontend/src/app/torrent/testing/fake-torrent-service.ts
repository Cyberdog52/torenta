import { inject, Injectable, Signal } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { DownloadDto } from '../../shared/dto/torrent/DownloadDto';
import { DownloadRequestDto } from '../../shared/dto/torrent/DownloadRequestDto';
import { TorrentEntry } from '../../shared/dto/pirateBay/TorrentEntry';

/**
 * A `TorrentService` replacement for tests that render a component depending on it.
 *
 * The real `TorrentService.downloads$` polls `api/bittorrent` forever via `timer(0, 1000)` with
 * real wall-clock timing. In a test, nothing flushes the *next* tick's HTTP request once the
 * first one is handled, which leaves a permanently pending request and hangs
 * `fixture.whenStable()` until the suite's timeout. This fake replaces `downloads$` with a single
 * empty, completed observable, while still using the real `HttpClient` (and thus
 * `HttpTestingController`) for `torrentSearchResource`/`startTorrent`, so those can still be
 * asserted against with `expectOne`/`flush` like any other request.
 */
@Injectable()
export class FakeTorrentService {
  private readonly httpClient = inject(HttpClient);

  readonly downloads$: Observable<DownloadDto[]> = of([]);

  startTorrent(downloadRequest: DownloadRequestDto): Observable<void> {
    return this.httpClient.post<void>('api/bittorrent', downloadRequest);
  }

  torrentSearchResource(searchString: Signal<string | undefined>) {
    return httpResource<TorrentEntry[]>(() => {
      const search = searchString()?.trim();
      return search ? { url: 'api/torrent', params: { search } } : undefined;
    });
  }
}
