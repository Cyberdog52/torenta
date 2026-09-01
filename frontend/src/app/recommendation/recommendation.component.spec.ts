import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { RecommendationComponent } from './recommendation.component';
import { DEFAULT_RECOMMENDATION_WEEKS } from './recommendation.service';

describe('RecommendationComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RecommendationComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
  });

  it('requests recommendations for the default number of weeks', async () => {
    const fixture = TestBed.createComponent(RecommendationComponent);
    fixture.detectChanges();

    const httpTesting = TestBed.inject(HttpTestingController);
    const request = httpTesting.expectOne(
      (r) => r.url === 'api/recommendation' && r.params.get('weeks') !== null,
    );
    expect(request.request.params.get('weeks')).toBe(String(DEFAULT_RECOMMENDATION_WEEKS));
    request.flush([]);
    await fixture.whenStable();

    httpTesting.verify();
  });

  it('renders recommended episodes grouped by series', async () => {
    const fixture = TestBed.createComponent(RecommendationComponent);
    fixture.detectChanges();

    const httpTesting = TestBed.inject(HttpTestingController);
    httpTesting
      .expectOne((r) => r.url === 'api/recommendation')
      .flush([
        {
          seriesName: 'The Office',
          tmdbSeriesId: 1,
          posterPath: '/office.jpg',
          recommendedEpisodes: [
            {
              seasonNumber: 3,
              episodeNumber: 5,
              episodeString: 'S03E05',
              name: 'Initiation',
              airDate: '2006-11-30',
              stillPath: null,
            },
          ],
        },
      ]);

    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.series-name')?.textContent).toContain('The Office');
    expect(compiled.querySelector('.episode-string')?.textContent).toContain('S03E05');
    const link = compiled.querySelector<HTMLAnchorElement>('.series-card a');
    expect(link?.getAttribute('href')).toContain('The%20Office');

    httpTesting.verify();
  });

  it('re-requests with the updated weeks value when the input changes', async () => {
    const fixture = TestBed.createComponent(RecommendationComponent);
    fixture.detectChanges();

    const httpTesting = TestBed.inject(HttpTestingController);
    httpTesting.expectOne((r) => r.url === 'api/recommendation').flush([]);
    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const input = compiled.querySelector<HTMLInputElement>('input[name="weeks"]');
    if (input == null) {
      throw new Error('Weeks input not found');
    }
    input.value = '4';
    input.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    const request = httpTesting.expectOne((r) => r.url === 'api/recommendation');
    expect(request.request.params.get('weeks')).toBe('4');
    request.flush([]);
    await fixture.whenStable();

    httpTesting.verify();
  });

  it('shows the empty state when there are no recommendations', async () => {
    const fixture = TestBed.createComponent(RecommendationComponent);
    fixture.detectChanges();

    const httpTesting = TestBed.inject(HttpTestingController);
    httpTesting.expectOne((r) => r.url === 'api/recommendation').flush([]);
    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.empty-title')?.textContent).toContain('caught up');

    httpTesting.verify();
  });
});
