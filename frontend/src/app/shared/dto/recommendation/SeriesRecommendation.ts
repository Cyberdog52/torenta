import { RecommendedEpisode } from './RecommendedEpisode';
import { TmdbSeriesDetailDto } from '../tmdb/TmdbSeriesDetailDto';

export interface SeriesRecommendation {
  seriesName: string;
  tmdbSeriesId: number;
  posterPath: string | null;
  recommendedEpisodes: RecommendedEpisode[];
  /** Needed to start a download for a torrent found for this series. */
  seriesDetail: TmdbSeriesDetailDto;
}
