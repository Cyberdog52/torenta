import {DirectoryDto} from "../directory/DirectoryDto";
import {TmdbSeriesDetailDto} from "../tmdb/TmdbSeriesDetailDto";
import {TmdbEpisodeDto} from "../tmdb/TmdbEpisodeDto";
import {Season} from "./Season";


export interface Series {
  directoryDto: DirectoryDto;
  seriesDetail: TmdbSeriesDetailDto;
  episodesBySeasonNumber: Map<number, TmdbEpisodeDto[]>;
  seasonDirectoriesBySeasonNumber: Map<number, DirectoryDto>;
  seasonList: Season[];
}
