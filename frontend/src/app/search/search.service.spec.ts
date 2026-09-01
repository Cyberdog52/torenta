import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SearchService } from './search.service';
import { ConciergeSearchResponseDto } from '../shared/dto/concierge/ConciergeSearchResponseDto';

describe('SearchService', () => {
  it('posts a concierge prompt and returns the response as an Observable', () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const service = TestBed.inject(SearchService);
    const httpTesting = TestBed.inject(HttpTestingController);
    const response = {
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
    let received: ConciergeSearchResponseDto | undefined;

    service.searchConcierge({ prompt: 'Something uplifting' }).subscribe((value) => {
      received = value;
    });

    const request = httpTesting.expectOne('api/concierge/search');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ prompt: 'Something uplifting' });
    request.flush(response);
    expect(received).toEqual(response);
    httpTesting.verify();
  });
});
