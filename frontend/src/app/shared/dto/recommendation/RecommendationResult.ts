import { SeriesRecommendation } from './SeriesRecommendation';

export interface RecommendationResult {
  seriesConsidered: number;
  unresolvedSeriesNames: string[];
  recommendations: SeriesRecommendation[];
}
