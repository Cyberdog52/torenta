import {TmdbCreatorDto} from "./TmdbCreatorDto";

export interface TmdbEpisodeDto {
  air_date: string;
  episode_number: number;
  id: number;
  name: string;
  overview: string;
  production_code: string;
  season_number: number;
  show_id: number;
  still_path?: any;
  vote_average: number;
  vote_count: number;
  crew: TmdbCreatorDto[];
  guest_stars: TmdbCreatorDto[];
}
