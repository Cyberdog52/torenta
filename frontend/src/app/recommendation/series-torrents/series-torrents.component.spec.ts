import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TorrentEntry } from '../../shared/dto/pirateBay/TorrentEntry';
import { SeriesRecommendation } from '../../shared/dto/recommendation/SeriesRecommendation';
import { RecommendedEpisode } from '../../shared/dto/recommendation/RecommendedEpisode';
import { TorrentService } from '../../torrent/torrent.service';
import { FakeTorrentService } from '../../torrent/testing/fake-torrent-service';
import { SeriesTorrentsComponent } from './series-torrents.component';

const seriesDetail = { id: 1, name: 'The Office' } as SeriesRecommendation['seriesDetail'];
const episode = {
  seasonNumber: 3,
  episodeNumber: 5,
  episodeString: 'S03E05',
  name: 'Initiation',
  airDate: '2006-11-30',
  stillPath: null,
  tmdbEpisodeDto: { season_number: 3, episode_number: 5 } as RecommendedEpisode['tmdbEpisodeDto'],
} as RecommendedEpisode;

function torrent(overrides: Partial<TorrentEntry> = {}): TorrentEntry {
  return {
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
    ...overrides,
  };
}

describe('SeriesTorrentsComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SeriesTorrentsComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: TorrentService, useClass: FakeTorrentService },
      ],
    }).compileComponents();
  });

  function createComponent() {
    const fixture = TestBed.createComponent(SeriesTorrentsComponent);
    fixture.componentRef.setInput('seriesDetail', seriesDetail);
    fixture.componentRef.setInput('episode', episode);
    return fixture;
  }

  it('shows a spinner while searching for torrents', () => {
    const fixture = createComponent();
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelector('mat-progress-spinner')).not.toBeNull();

    TestBed.inject(HttpTestingController)
      .expectOne((r) => r.url === 'api/torrent')
      .flush([]);
  });

  it('shows only trusted/VIP torrents sorted by seeders, capped to 3', async () => {
    const fixture = createComponent();
    fixture.detectChanges();

    const httpTesting = TestBed.inject(HttpTestingController);
    httpTesting
      .expectOne((r) => r.url === 'api/torrent')
      .flush([
        torrent({
          name: 'Untrusted',
          uploaderIsTrusted: false,
          uploaderIsVIP: false,
          numberOfSeeders: 999,
        }),
        torrent({ name: 'Trusted low seeders', numberOfSeeders: 5 }),
        torrent({ name: 'Trusted high seeders', numberOfSeeders: 50 }),
        torrent({
          name: 'VIP mid seeders',
          uploaderIsTrusted: false,
          uploaderIsVIP: true,
          numberOfSeeders: 20,
        }),
        torrent({ name: 'Trusted extra', numberOfSeeders: 30 }),
      ]);
    await fixture.whenStable();
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    const names = Array.from(host.querySelectorAll('.torrent-name')).map((el) => el.textContent);
    expect(names).toEqual(['Trusted high seeders', 'Trusted extra', 'VIP mid seeders']);
  });

  it('shows a message when there are no trusted/VIP torrents', async () => {
    const fixture = createComponent();
    fixture.detectChanges();

    const httpTesting = TestBed.inject(HttpTestingController);
    httpTesting
      .expectOne((r) => r.url === 'api/torrent')
      .flush([torrent({ uploaderIsTrusted: false, uploaderIsVIP: false })]);
    await fixture.whenStable();
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelector('.no-results')?.textContent).toContain('No trusted torrents found');
  });

  it('starts a download and shows pending state', async () => {
    const fixture = createComponent();
    fixture.detectChanges();

    const httpTesting = TestBed.inject(HttpTestingController);
    httpTesting.expectOne((r) => r.url === 'api/torrent').flush([torrent()]);
    await fixture.whenStable();
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    const button = host.querySelector<HTMLButtonElement>('[aria-label="Download Example torrent"]');
    if (button == null) {
      throw new Error('Download button not found');
    }

    button.click();
    fixture.detectChanges();

    expect(button.disabled).toBe(true);
    expect(button.textContent).toContain('Starting');
    httpTesting.expectOne((r) => r.url === 'api/bittorrent').flush(null);
    httpTesting.verify();
  });
});
