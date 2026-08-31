import { TmdbEpisodeDto } from './TmdbEpisodeDto';

export interface TmdbSeasonDto {
  air_date: string | null;
  episode_count: number;
  id: number;
  name: string;
  overview: string;
  poster_path: string | null;
  season_number: number;
  episodes: TmdbEpisodeDto[];
}
