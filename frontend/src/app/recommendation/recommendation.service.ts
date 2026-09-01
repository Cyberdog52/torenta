import { Injectable, Signal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { RecommendationResult } from '../shared/dto/recommendation/RecommendationResult';

const BACKEND_URL = 'api/recommendation';

/**
 * Only series folders touched within this many weeks are scanned by default, to keep the scan
 * fast for large libraries. Set to `0` in the UI to scan the whole library regardless of when a
 * series was last touched (useful for older, genuinely-incomplete downloads that a recency
 * filter would otherwise hide from the very feature meant to surface them).
 */
export const DEFAULT_RECOMMENDATION_WEEKS = 2;

@Injectable({ providedIn: 'root' })
export class RecommendationService {
  /** Reactive recommendation result, refetched whenever `weeks` changes. */
  recommendationsResource(weeks: Signal<number>) {
    return httpResource<RecommendationResult>(() => ({
      url: BACKEND_URL,
      params: { weeks: weeks() },
    }));
  }
}
