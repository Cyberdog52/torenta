import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { RecommendationComponent } from './recommendation.component';
import { DEFAULT_RECOMMENDATION_DAYS } from './recommendation.service';
import { RecommendationResult } from '../shared/dto/recommendation/RecommendationResult';
import { SeriesRecommendation } from '../shared/dto/recommendation/SeriesRecommendation';
import { RecommendedEpisode } from '../shared/dto/recommendation/RecommendedEpisode';
import { TorrentService } from '../torrent/torrent.service';
import { FakeTorrentService } from '../torrent/testing/fake-torrent-service';

function result(overrides: Partial<RecommendationResult> = {}): RecommendationResult {
  return {
    seriesConsidered: 0,
    unresolvedSeriesNames: [],
    recommendations: [],
    ...overrides,
  };
}

describe('RecommendationComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RecommendationComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: TorrentService, useClass: FakeTorrentService },
      ],
    }).compileComponents();
  });

  it('requests recommendations for the default number of days', async () => {
    const fixture = TestBed.createComponent(RecommendationComponent);
    fixture.detectChanges();

    const httpTesting = TestBed.inject(HttpTestingController);
    const request = httpTesting.expectOne(
      (r) => r.url === 'api/recommendation' && r.params.get('days') !== null,
    );
    expect(request.request.params.get('days')).toBe(String(DEFAULT_RECOMMENDATION_DAYS));
    request.flush(result());
    await fixture.whenStable();

    httpTesting.verify();
  });

  it('renders recommended episodes grouped by series', async () => {
    const fixture = TestBed.createComponent(RecommendationComponent);
    fixture.detectChanges();

    const httpTesting = TestBed.inject(HttpTestingController);
    httpTesting
      .expectOne((r) => r.url === 'api/recommendation')
      .flush(
        result({
          seriesConsidered: 1,
          recommendations: [
            {
              seriesName: 'The Office',
              tmdbSeriesId: 1,
              posterPath: '/office.jpg',
              seriesDetail: { id: 1, name: 'The Office' } as SeriesRecommendation['seriesDetail'],
              recommendedEpisodes: [
                {
                  seasonNumber: 3,
                  episodeNumber: 5,
                  episodeString: 'S03E05',
                  name: 'Initiation',
                  airDate: '2006-11-30',
                  stillPath: null,
                  tmdbEpisodeDto: {
                    season_number: 3,
                    episode_number: 5,
                  } as RecommendedEpisode['tmdbEpisodeDto'],
                },
              ],
            },
          ],
        }),
      );

    // Flush both the recommendation request and the torrent search triggered by the embedded
    // <app-series-torrents> using plain synchronous detectChanges() cycles, *before* awaiting
    // whenStable(): the child's torrent search is itself tracked as a pending task, so awaiting
    // stability before it's flushed would deadlock forever.
    fixture.detectChanges();
    await Promise.resolve();
    fixture.detectChanges();
    httpTesting.expectOne((r) => r.url === 'api/torrent').flush([]);

    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.series-name')?.textContent).toContain('The Office');
    expect(compiled.querySelector('.episode-string')?.textContent).toContain('S03E05');
    expect(compiled.querySelector('.scan-summary')?.textContent).toContain('1 series folder');
    const link = compiled.querySelector<HTMLAnchorElement>('.series-card a');
    expect(link?.getAttribute('href')).toContain('The%20Office');

    httpTesting.verify();
  });

  it('surfaces series that could not be matched to TMDB', async () => {
    const fixture = TestBed.createComponent(RecommendationComponent);
    fixture.detectChanges();

    const httpTesting = TestBed.inject(HttpTestingController);
    httpTesting
      .expectOne((r) => r.url === 'api/recommendation')
      .flush(result({ seriesConsidered: 1, unresolvedSeriesNames: ['Ambiguous Show'] }));

    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.unresolved-warning')?.textContent).toContain('Ambiguous Show');

    httpTesting.verify();
  });

  it('re-requests with the updated days value when the input changes', async () => {
    const fixture = TestBed.createComponent(RecommendationComponent);
    fixture.detectChanges();

    const httpTesting = TestBed.inject(HttpTestingController);
    httpTesting.expectOne((r) => r.url === 'api/recommendation').flush(result());
    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const input = compiled.querySelector<HTMLInputElement>('input[name="days"]');
    if (input == null) {
      throw new Error('Days input not found');
    }
    input.value = '4';
    input.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    const request = httpTesting.expectOne((r) => r.url === 'api/recommendation');
    expect(request.request.params.get('days')).toBe('4');
    request.flush(result());
    await fixture.whenStable();

    httpTesting.verify();
  });

  it('accepts 0 days (scan entire library) from the input', async () => {
    const fixture = TestBed.createComponent(RecommendationComponent);
    fixture.detectChanges();

    const httpTesting = TestBed.inject(HttpTestingController);
    httpTesting.expectOne((r) => r.url === 'api/recommendation').flush(result());
    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const input = compiled.querySelector<HTMLInputElement>('input[name="days"]');
    if (input == null) {
      throw new Error('Days input not found');
    }
    input.value = '4';
    input.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    httpTesting.expectOne((r) => r.url === 'api/recommendation').flush(result());
    await fixture.whenStable();
    fixture.detectChanges();

    input.value = '0';
    input.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    const request = httpTesting.expectOne((r) => r.url === 'api/recommendation');
    expect(request.request.params.get('days')).toBe('0');
    request.flush(result());
    await fixture.whenStable();

    httpTesting.verify();
  });

  it('shows the empty state when there are no recommendations', async () => {
    const fixture = TestBed.createComponent(RecommendationComponent);
    fixture.detectChanges();

    const httpTesting = TestBed.inject(HttpTestingController);
    httpTesting
      .expectOne((r) => r.url === 'api/recommendation')
      .flush(result({ seriesConsidered: 5 }));
    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.empty-title')?.textContent).toContain('caught up');
    expect(compiled.querySelector('.empty-subtitle')?.textContent).toContain('5 series folder');

    httpTesting.verify();
  });

  it('tells the user no series folders were found when nothing was scanned', async () => {
    const fixture = TestBed.createComponent(RecommendationComponent);
    fixture.detectChanges();

    const httpTesting = TestBed.inject(HttpTestingController);
    httpTesting.expectOne((r) => r.url === 'api/recommendation').flush(result());
    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.empty-subtitle')?.textContent).toContain(
      'No series folders were found',
    );

    httpTesting.verify();
  });
});
