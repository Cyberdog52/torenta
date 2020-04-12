import {TmdbEpisodeDto} from "../tmdb/TmdbEpisodeDto";
import {TorrentEntry} from "../pirateBay/TorrentEntry";
import {TmdbSeriesDetailDto} from "../tmdb/TmdbSeriesDetailDto";

export interface DownloadRequest {
  tmdbEpisodeDto: TmdbEpisodeDto;
  seriesDetail: TmdbSeriesDetailDto;
  torrentEntry: TorrentEntry;
}
