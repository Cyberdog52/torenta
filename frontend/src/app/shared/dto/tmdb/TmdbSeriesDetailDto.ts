import {TmdbCreatorDto} from "./TmdbCreatorDto";
import {TmdbEpisodeDto} from "./TmdbEpisodeDto";
import {TmdbGenreDto} from "./TmdbGenreDto";
import {TmdbNetworkDto} from "./TmdbNetworkDto";
import {TmdbSeasonDto} from "./TmdbSeasonDto";
import {TmdbProductionCompanyDto} from "./TmdbProductionCompanyDto";

export interface TmdbSeriesDetailDto {
  backdrop_path: string;
  created_by: TmdbCreatorDto[];
  episode_run_time: number[];
  first_air_date: string;
  genres: TmdbGenreDto[];
  homepage: string;
  id: number;
  in_production: boolean;
  languages: string[];
  last_air_date: string;
  last_episode_to_air: TmdbEpisodeDto;
  name: string;
  next_episode_to_air: TmdbEpisodeDto;
  networks: TmdbNetworkDto[];
  number_of_episodes: number;
  number_of_seasons: number;
  origin_country: string[];
  original_language: string;
  original_name: string;
  overview: string;
  popularity: number;
  poster_path: string;
  production_companies: TmdbProductionCompanyDto[];
  seasons: TmdbSeasonDto[];
  status: string;
  type: string;
  vote_average: number;
  vote_count: number;
}
