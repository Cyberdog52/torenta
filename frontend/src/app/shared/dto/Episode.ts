import {Creator} from "./Creator";

export interface Episode {
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
  crew: Creator[];
  guest_stars: Creator[];
}
