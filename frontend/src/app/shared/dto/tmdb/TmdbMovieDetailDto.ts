import {TmdbGenreDto} from "./TmdbGenreDto";
import {TmdbProductionCompanyDto} from "./TmdbProductionCompanyDto";
import {TmdbSpokenLanguagesDto} from "./TmdbSpokenLanguagesDto";

export interface TmdbMovieDetailDto {
  adult: boolean;
  backdrop_path: string;
  budget: number;
  genres: TmdbGenreDto[];
  homepage: string;
  id: number;
  imdb_id: string;
  original_language: string;
  original_title: string;
  overview: string;
  popularity: number;
  poster_path: string;
  production_companies: TmdbProductionCompanyDto[];
  release_date: string;
  revenue: number;
  runtime: number;
  spoken_languages: TmdbSpokenLanguagesDto[];
  status: string;
  tagline: string;
  title: string;
  video: boolean;
  vote_average: number;
  vote_count: number;
}
