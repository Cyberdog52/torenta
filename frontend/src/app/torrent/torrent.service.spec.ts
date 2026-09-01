import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TorrentService } from './torrent.service';

describe('TorrentService lifecycle actions', () => {
  let service: TorrentService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TorrentService);
    http = TestBed.inject(HttpTestingController);
  });

  it.each([
    ['pauseTorrent', 'POST', 'api/bittorrent/download-id/pause'],
    ['restartTorrent', 'POST', 'api/bittorrent/download-id/restart'],
    ['stopAndDeleteTorrent', 'DELETE', 'api/bittorrent/download-id'],
    ['removeTorrentTile', 'DELETE', 'api/bittorrent/download-id/tile'],
  ] as const)('sends %s to the lifecycle endpoint', (method, verb, url) => {
    service[method]('download-id').subscribe();

    const request = http.expectOne(url);
    expect(request.request.method).toBe(verb);
    request.flush(null);
  });

  afterEach(() => http.verify());
});
