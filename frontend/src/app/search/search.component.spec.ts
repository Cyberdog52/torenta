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
});
