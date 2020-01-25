import {TmdbEpisodeDto} from "../tmdb/TmdbEpisodeDto";
import {PirateBayEntry} from "../pirateBay/PirateBayEntry";
import {TmdbSeriesDetailDto} from "../tmdb/TmdbSeriesDetailDto";

export interface DownloadRequest {
  tmdbEpisodeDto: TmdbEpisodeDto;
  seriesDetail: TmdbSeriesDetailDto;
  pirateBayEntry: PirateBayEntry;
}
