import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SearchComponent } from './search.component';

describe('SearchComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SearchComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  it('renders the three search cards without firing requests', async () => {
    const fixture = TestBed.createComponent(SearchComponent);
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelectorAll('mat-card')).toHaveLength(3);

    // Empty queries must keep the resources idle.
    TestBed.inject(HttpTestingController).verify();
  });

  it('sorts series results by popularity, descending', async () => {
    vi.useFakeTimers();
    try {
      const fixture = TestBed.createComponent(SearchComponent);
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const seriesInput = compiled.querySelector<HTMLInputElement>(
        '.search-card:nth-of-type(1) input[matInput]',
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
        { id: 1, name: 'Show A', popularity: 5, backdrop_path: '/show-a.jpg' },
        { id: 2, name: 'Show B', popularity: 50, backdrop_path: '/show-b.jpg' },
        { id: 3, name: 'Show C', popularity: 20, backdrop_path: null },
      ];
      request.flush({ results });

      vi.useRealTimers();
      await fixture.whenStable();
      fixture.detectChanges();

      const names = Array.from(compiled.querySelectorAll('.result-name')).map((element) =>
        element.textContent?.trim(),
      );
      expect(names).toEqual(['Show B', 'Show C', 'Show A']);

      const backdrops = Array.from(compiled.querySelectorAll<HTMLElement>('.result-panel')).map(
        (element) => element.style.getPropertyValue('--media-backdrop-image'),
      );
      expect(backdrops).toEqual(['', '', '']);

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

      const input = compiled.querySelector<HTMLInputElement>(
        '.search-card:nth-of-type(2) input[matInput]',
      );
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

      const panel = compiled.querySelector<HTMLElement>(
        '.search-card:nth-of-type(2) .result-panel',
      );
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
      const input = compiled.querySelector<HTMLInputElement>(
        '.search-card:nth-of-type(1) input[matInput]',
      );
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

  it('clears a search input and removes its result list', async () => {
    vi.useFakeTimers();
    try {
      const fixture = TestBed.createComponent(SearchComponent);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      const input = compiled.querySelector<HTMLInputElement>(
        '.search-card:nth-of-type(1) input[matInput]',
      );
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
