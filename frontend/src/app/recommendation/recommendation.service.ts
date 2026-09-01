import { Injectable, Signal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { SeriesRecommendation } from '../shared/dto/recommendation/SeriesRecommendation';

const BACKEND_URL = 'api/recommendation';

export const DEFAULT_RECOMMENDATION_WEEKS = 2;

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
