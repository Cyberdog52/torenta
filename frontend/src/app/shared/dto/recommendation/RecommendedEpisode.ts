import { TmdbEpisodeDto } from '../tmdb/TmdbEpisodeDto';

export interface RecommendedEpisode {
  seasonNumber: number;
  episodeNumber: number;
  episodeString: string;
  name: string;
  airDate: string;
  stillPath: string | null;
  /** Needed to start a download for a torrent found for this episode. */
  tmdbEpisodeDto: TmdbEpisodeDto;
}
