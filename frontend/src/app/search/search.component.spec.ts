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
    const fixture = TestBed.createComponent(SearchComponent);
    const component = fixture.componentInstance as unknown as {
      seriesQuery: { set: (value: string) => void };
      seriesOverviews: () => { id: number; popularity: number }[];
    };

    component.seriesQuery.set('andor');
    fixture.detectChanges();

    const httpTesting = TestBed.inject(HttpTestingController);
    const request = httpTesting.expectOne((r) => r.url === 'api/tmdb/tv');
    expect(request.request.params.get('search')).toBe('andor');

    const results = [
      { id: 1, popularity: 5 },
      { id: 2, popularity: 50 },
      { id: 3, popularity: 20 },
    ];
    request.flush({ results });
    await fixture.whenStable();

    expect(component.seriesOverviews().map((s) => s.id)).toEqual([2, 3, 1]);
    // The source array must not be mutated by the sort.
    expect(results.map((s) => s.id)).toEqual([1, 2, 3]);

    httpTesting.verify();
  });
});
