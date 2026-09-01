import { RecommendedEpisode } from './RecommendedEpisode';

export interface SeriesRecommendation {
  seriesName: string;
  tmdbSeriesId: number;
  posterPath: string | null;
  recommendedEpisodes: RecommendedEpisode[];
}
