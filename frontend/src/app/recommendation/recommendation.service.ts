import { Injectable, Signal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { SeriesRecommendation } from '../shared/dto/recommendation/SeriesRecommendation';

const BACKEND_URL = 'api/recommendation';

/**
 * `0` means "no filter": scan every series regardless of when it was last touched. This is the
 * default because a series can be genuinely incomplete but untouched for a long time (e.g. an
 * old download that was never finished), which a recency filter would otherwise hide from the
 * very feature meant to surface it. Set to a positive number of weeks to speed up the scan for
 * very large libraries by only considering recently touched series.
 */
export const DEFAULT_RECOMMENDATION_WEEKS = 0;

@Injectable({ providedIn: 'root' })
export class RecommendationService {
  /** Reactive list of series recommendations, refetched whenever `weeks` changes. */
  recommendationsResource(weeks: Signal<number>) {
    return httpResource<SeriesRecommendation[]>(() => ({
      url: BACKEND_URL,
      params: { weeks: weeks() },
    }));
  }
}
