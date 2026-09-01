import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TorrentEntry } from '../../shared/dto/pirateBay/TorrentEntry';
import { TorrentSuggestionsComponent } from './torrent-suggestions.component';

const torrent: TorrentEntry = {
  name: 'Example torrent',
  magnetLink: 'magnet:?xt=urn:btih:example',
  link: '/torrent/example',
  uploadedTime: 'Today',
  size: '1 GiB',
  uploader: 'example',
  numberOfSeeders: 10,
  numberOfLeechers: 1,
  category: 'Video',
  subCategory: 'HD',
  uploaderIsVIP: false,
  uploaderIsTrusted: true,
};

describe('TorrentSuggestionsComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TorrentSuggestionsComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  it('disables a download immediately and shows pending progress', async () => {
    vi.useFakeTimers();
    try {
      const fixture = TestBed.createComponent(TorrentSuggestionsComponent);
      fixture.componentRef.setInput('searchString', 'example');
      fixture.detectChanges();
      vi.advanceTimersByTime(0);
      fixture.detectChanges();

      const httpTesting = TestBed.inject(HttpTestingController);
      httpTesting.expectOne('api/bittorrent').flush([]);
      httpTesting
        .expectOne(
          (request) => request.url === 'api/torrent' && request.params.get('search') === 'example',
        )
        .flush([torrent]);

      vi.useRealTimers();
      await fixture.whenStable();
      fixture.detectChanges();

      const host = fixture.nativeElement as HTMLElement;
      const button = host.querySelector<HTMLButtonElement>(
        '[aria-label="Download Example torrent"]',
      );
      if (button == null) {
        throw new Error('Download button not found');
      }

      button.click();
      fixture.detectChanges();

      expect(button.disabled).toBe(true);
      expect(button.textContent).toContain('Starting');
      expect(button.querySelector('mat-progress-spinner')).not.toBeNull();
      httpTesting.expectOne('api/bittorrent').flush(null);
      httpTesting.verify();
    } finally {
      vi.useRealTimers();
    }
  });
});
