import {Creator} from "./Creator";
import {Episode} from "./Episode";
import {Genre} from "./Genre";
import {Network} from "./Network";
import {Season} from "./Season";
import {ProductionCompany} from "./ProductionCompany";

export interface SeriesDetail {
  backdrop_path: string;
  created_by: Creator[];
  episode_run_time: number[];
  first_air_date: string;
  genres: Genre[];
  homepage: string;
  id: number;
  in_production: boolean;
  languages: string[];
  last_air_date: string;
  last_episode_to_air: Episode;
  name: string;
  next_episode_to_air: Episode;
  networks: Network[];
  number_of_episodes: number;
  number_of_seasons: number;
  origin_country: string[];
  original_language: string;
  original_name: string;
  overview: string;
  popularity: number;
  poster_path: string;
  production_companies: ProductionCompany[];
  seasons: Season[];
  status: string;
  type: string;
  vote_average: number;
  vote_count: number;
}
