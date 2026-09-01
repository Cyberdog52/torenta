import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { SearchComponent } from './search.component';
import { ComponentFixture } from '@angular/core/testing';
import { ConciergeSearchResponseDto } from '../shared/dto/concierge/ConciergeSearchResponseDto';

const EMPTY_RESPONSE = {
  intent: {
    mediaType: 'ANY',
    moods: [],
    similarTo: null,
    numericFilters: [],
    dateFilters: [],
    textFilters: [],
    booleanFilters: [],
    namedFilters: [],
    enumFilters: [],
  },
  results: [],
} satisfies ConciergeSearchResponseDto;

function conciergeInput(fixture: ComponentFixture<SearchComponent>): HTMLInputElement {
  const root = fixture.nativeElement as HTMLElement;
  const input = root.querySelector<HTMLInputElement>('.concierge-card input');
  if (input == null) {
    throw new Error('Concierge input not found');
  }
  return input;
}

function submitConcierge(
  fixture: ComponentFixture<SearchComponent>,
  prompt: string,
): HTMLInputElement {
  const input = conciergeInput(fixture);
  input.value = prompt;
  input.closest('form')?.requestSubmit();
  fixture.detectChanges();
  return input;
}

describe('SearchComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SearchComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
  });

  it('renders the concierge before the three existing search cards without firing requests', async () => {
    const fixture = TestBed.createComponent(SearchComponent);
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelectorAll('mat-card')).toHaveLength(4);
    expect(compiled.querySelector('mat-card h2')?.textContent).toContain('AI Concierge');
    expect(conciergeInput(fixture).getAttribute('placeholder')).toContain('For example:');

    // Empty queries must keep the resources idle.
    TestBed.inject(HttpTestingController).verify();
  });

  it('submits the same trimmed prompt through Enter and the Search button', () => {
    const fixture = TestBed.createComponent(SearchComponent);
    fixture.detectChanges();
    const httpTesting = TestBed.inject(HttpTestingController);
    const input = conciergeInput(fixture);

    input.value = '  clever science fiction  ';
    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
    input.closest('form')?.requestSubmit();
    const enterRequest = httpTesting.expectOne('api/concierge/search');
    expect(enterRequest.request.body).toEqual({ prompt: 'clever science fiction' });
    enterRequest.flush(EMPTY_RESPONSE);

    input.value = '  clever science fiction  ';
    const root = fixture.nativeElement as HTMLElement;
    const button = root.querySelector<HTMLButtonElement>('.concierge-card button[type="submit"]');
    button?.click();
    const clickRequest = httpTesting.expectOne((request) => request.url === 'api/concierge/search');
    expect(clickRequest.request.body).toEqual(enterRequest.request.body);
    clickRequest.flush(EMPTY_RESPONSE);

    httpTesting.verify();
  });

  it('shows loading, disables Search, and cancels a stale request when replaced', () => {
    const fixture = TestBed.createComponent(SearchComponent);
    fixture.detectChanges();
    const httpTesting = TestBed.inject(HttpTestingController);

    submitConcierge(fixture, 'first prompt');
    const first = httpTesting.expectOne('api/concierge/search');
    const root = fixture.nativeElement as HTMLElement;
    const button = root.querySelector<HTMLButtonElement>('.concierge-card button[type="submit"]');
    expect(button?.disabled).toBe(true);
    expect(root.textContent).toContain('Finding recommendations');

    submitConcierge(fixture, 'replacement prompt');
    expect(first.cancelled).toBe(true);
    const replacement = httpTesting.expectOne('api/concierge/search');
    expect(replacement.request.body).toEqual({ prompt: 'replacement prompt' });
    replacement.flush(EMPTY_RESPONSE);
    fixture.detectChanges();
    expect(button?.disabled).toBe(false);
    httpTesting.verify();
  });

  it('renders ranked mixed-media results and explanations without torrent requests', () => {
    const fixture = TestBed.createComponent(SearchComponent);
    fixture.detectChanges();
    const httpTesting = TestBed.inject(HttpTestingController);
    submitConcierge(fixture, 'surprise me');
    httpTesting.expectOne('api/concierge/search').flush({
      ...EMPTY_RESPONSE,
      results: [
        {
          rank: 2,
          mediaType: 'MOVIE',
          id: 7,
          title: 'Second Movie',
          overview: 'Movie overview',
          posterPath: null,
          releaseDate: '2020-01-01',
          rating: 7.5,
          explanation: 'A thoughtful second choice.',
        },
        {
          rank: 1,
          mediaType: 'SERIES',
          id: 7,
          title: 'First Series',
          overview: 'Series overview',
          posterPath: null,
          releaseDate: '2021-01-01',
          rating: 8.8,
          explanation: 'The strongest match for your mood.',
        },
      ],
    } satisfies ConciergeSearchResponseDto);
    fixture.detectChanges();

    const root = fixture.nativeElement as HTMLElement;
    const panels = Array.from(root.querySelectorAll<HTMLElement>('.concierge-result-panel'));
    expect(panels).toHaveLength(2);
    expect(panels.map((panel) => panel.querySelector('.result-name')?.textContent?.trim())).toEqual(
      ['First Series', 'Second Movie'],
    );
    expect(panels[0].textContent).toContain('The strongest match for your mood.');
    expect(panels[1].textContent).toContain('A thoughtful second choice.');
    expect(
      httpTesting.match(
        (request) =>
          request.url.startsWith('api/torrent') || request.url.startsWith('api/bittorrent'),
      ),
    ).toHaveLength(0);
    httpTesting.verify();
  });

  it('keeps concierge and manual search panel state independent for the same media id', () => {
    const fixture = TestBed.createComponent(SearchComponent);
    const component = fixture.componentInstance as unknown as {
      panelKey(mediaType: 'MOVIE' | 'SERIES', id: number): string;
      conciergePanelKey(result: { mediaType: 'MOVIE' | 'SERIES'; id: number }): string;
    };

    expect(component.panelKey('MOVIE', 7)).not.toBe(
      component.conciergePanelKey({ mediaType: 'MOVIE', id: 7 }),
    );
  });

  it('shows explicit empty, validation, and backend error states', () => {
    const fixture = TestBed.createComponent(SearchComponent);
    fixture.detectChanges();
    const httpTesting = TestBed.inject(HttpTestingController);
    const root = fixture.nativeElement as HTMLElement;

    submitConcierge(fixture, 'no matches');
    httpTesting.expectOne('api/concierge/search').flush(EMPTY_RESPONSE);
    fixture.detectChanges();
    expect(root.textContent).toContain('No recommendations matched');

    submitConcierge(fixture, ' ');
    expect(root.querySelector('[role="alert"]')?.textContent).toContain('Please describe');

    submitConcierge(fixture, 'backend fails');
    httpTesting
      .expectOne('api/concierge/search')
      .flush({ message: 'failed' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect(root.querySelector('[role="alert"]')?.textContent).toContain('could not complete');
    httpTesting.verify();
  });

  it('stops loading when the concierge request times out', () => {
    vi.useFakeTimers();
    try {
      const fixture = TestBed.createComponent(SearchComponent);
      fixture.detectChanges();
      const httpTesting = TestBed.inject(HttpTestingController);
      const root = fixture.nativeElement as HTMLElement;

      submitConcierge(fixture, 'a horror movie with clowns');
      const request = httpTesting.expectOne('api/concierge/search');

      vi.advanceTimersByTime(130_000);
      fixture.detectChanges();

      expect(request.cancelled).toBe(true);
      expect(root.textContent).not.toContain('Finding recommendations');
      expect(root.querySelector('[role="alert"]')?.textContent).toContain('could not complete');
      httpTesting.verify();
    } finally {
      vi.useRealTimers();
    }
  });

  it('sorts series results by popularity, descending', async () => {
    vi.useFakeTimers();
    try {
      const fixture = TestBed.createComponent(SearchComponent);
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const seriesInput = compiled.querySelector<HTMLInputElement>(
        '.series-search-card input[matInput]',
      );
      if (seriesInput == null) {
        throw new Error('Series search input not found');
      }

      // Drive the search through the DOM, like a user typing, instead of
      // reaching into the component's private signals.
      seriesInput.value = 'andor';
      seriesInput.dispatchEvent(new KeyboardEvent('keyup'));

      // DelayedKeyupDirective debounces by 300ms before emitting.
      vi.advanceTimersByTime(300);
      fixture.detectChanges();

      const httpTesting = TestBed.inject(HttpTestingController);
      const request = httpTesting.expectOne((r) => r.url === 'api/tmdb/tv');
      expect(request.request.params.get('search')).toBe('andor');

      const results = [
        { id: 1, name: 'Show A', popularity: 5 },
        { id: 2, name: 'Show B', popularity: 50 },
        { id: 3, name: 'Show C', popularity: 20 },
      ];
      request.flush({ results });

      vi.useRealTimers();
      await fixture.whenStable();
      fixture.detectChanges();

      const names = Array.from(compiled.querySelectorAll('.result-name')).map((element) =>
        element.textContent?.trim(),
      );
      expect(names).toEqual(['Show B', 'Show C', 'Show A']);

      // The source array must not be mutated by the sort.
      expect(results.map((s) => s.id)).toEqual([1, 2, 3]);

      httpTesting.verify();
    } finally {
      vi.useRealTimers();
    }
  });
  it('does not set the movie backdrop while its result is collapsed', async () => {
    vi.useFakeTimers();
    try {
      const fixture = TestBed.createComponent(SearchComponent);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;

      const input = compiled.querySelector<HTMLInputElement>('.movie-search-card input[matInput]');
      if (input == null) {
        throw new Error('Movie search input not found');
      }

      input.value = 'arrival';
      input.dispatchEvent(new KeyboardEvent('keyup'));
      vi.advanceTimersByTime(300);
      fixture.detectChanges();

      const httpTesting = TestBed.inject(HttpTestingController);
      httpTesting
        .expectOne((request) => request.url === 'api/tmdb/movie')
        .flush({
          results: [
            {
              id: 4,
              original_title: 'Arrival',
              popularity: 80,
              backdrop_path: '/arrival.jpg',
            },
          ],
        });

      vi.useRealTimers();
      await fixture.whenStable();
      fixture.detectChanges();

      const panel = compiled.querySelector<HTMLElement>('.movie-search-card .result-panel');
      expect(panel?.style.getPropertyValue('--media-backdrop-image')).toBe('');
      httpTesting.verify();
    } finally {
      vi.useRealTimers();
    }
  });

  it('shows series metadata and overview control only while expanded', async () => {
    vi.useFakeTimers();
    try {
      const fixture = TestBed.createComponent(SearchComponent);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      const input = compiled.querySelector<HTMLInputElement>('.series-search-card input[matInput]');
      if (input == null) {
        throw new Error('Series search input not found');
      }

      input.value = 'andor';
      input.dispatchEvent(new KeyboardEvent('keyup'));
      vi.advanceTimersByTime(300);
      fixture.detectChanges();

      const httpTesting = TestBed.inject(HttpTestingController);
      httpTesting
        .expectOne((request) => request.url === 'api/tmdb/tv')
        .flush({
          results: [
            {
              id: 7,
              name: 'Andor',
              popularity: 90,
              poster_path: '/andor-poster.jpg',
              backdrop_path: '/andor.jpg',
            },
          ],
        });

      vi.useRealTimers();
      await fixture.whenStable();
      fixture.detectChanges();
      expect(compiled.querySelector('.result-summary')).toBeNull();

      const header = compiled.querySelector<HTMLElement>('mat-expansion-panel-header');
      if (header == null) {
        throw new Error('Series result header not found');
      }
      header.click();
      fixture.detectChanges();

      httpTesting.expectOne('api/tmdb/tv/7').flush({
        id: 7,
        name: 'Andor',
        overview: 'A rebellion begins.',
        first_air_date: '2022-09-21',
        episode_run_time: [],
        vote_average: 8.2,
        genres: [{ id: 18, name: 'Drama' }],
        seasons: [],
      });
      fixture.detectChanges();
      await Promise.resolve();
      fixture.detectChanges();
      httpTesting.expectOne('api/directory/series/Andor').flush({
        name: 'Andor',
        absolutePath: '/media/Andor',
        files: [],
        directories: [],
      });
      await fixture.whenStable();
      fixture.detectChanges();

      expect(compiled.querySelector('.result-summary')?.textContent).toContain('8.2');
      expect(compiled.querySelector('.result-summary')?.textContent).toContain('Drama');
      expect(compiled.querySelector('.result-summary')?.textContent).not.toContain('min');
      expect(compiled.querySelector('.overview-trigger')).not.toBeNull();
      expect(compiled.querySelector('.series-detail .overview')).toBeNull();

      header.click();
      await fixture.whenStable();
      fixture.detectChanges();
      expect(compiled.querySelector('.result-summary')).toBeNull();
      httpTesting.verify();
    } finally {
      vi.useRealTimers();
    }
  });

  it('prefills the series search from a "series" query param', async () => {
    TestBed.overrideProvider(ActivatedRoute, {
      useValue: {
        snapshot: { queryParamMap: convertToParamMap({ series: 'The Office' }) },
      },
    });

    const fixture = TestBed.createComponent(SearchComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    const input = compiled.querySelector<HTMLInputElement>('.series-search-card input[matInput]');
    expect(input?.value).toBe('The Office');

    const httpTesting = TestBed.inject(HttpTestingController);
    httpTesting.expectOne((r) => r.url === 'api/tmdb/tv').flush({ results: [] });
    await fixture.whenStable();
    httpTesting.verify();
  });

  it('clears a search input and removes its result list', async () => {
    vi.useFakeTimers();
    try {
      const fixture = TestBed.createComponent(SearchComponent);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      const input = compiled.querySelector<HTMLInputElement>('.series-search-card input[matInput]');
      if (input == null) {
        throw new Error('Series search input not found');
      }

      input.value = 'andor';
      input.dispatchEvent(new InputEvent('input'));
      input.dispatchEvent(new KeyboardEvent('keyup'));
      fixture.detectChanges();
      expect(compiled.querySelector('[aria-label="Clear TV show search"]')).not.toBeNull();

      vi.advanceTimersByTime(300);
      fixture.detectChanges();
      const httpTesting = TestBed.inject(HttpTestingController);
      httpTesting
        .expectOne((request) => request.url === 'api/tmdb/tv')
        .flush({
          results: [
            {
              id: 7,
              name: 'Andor',
              popularity: 90,
              poster_path: '/andor-poster.jpg',
              backdrop_path: '/andor.jpg',
            },
          ],
        });

      vi.useRealTimers();
      await fixture.whenStable();
      fixture.detectChanges();
      expect(compiled.querySelector('.result-panel')).not.toBeNull();

      compiled.querySelector<HTMLButtonElement>('[aria-label="Clear TV show search"]')?.click();
      fixture.detectChanges();

      expect(input.value).toBe('');
      expect(compiled.querySelector('.result-panel')).toBeNull();
      expect(document.activeElement).toBe(input);
      httpTesting.verify();
    } finally {
      vi.useRealTimers();
    }
  });
});
