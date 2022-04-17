import {TmdbEpisodeDto} from "../tmdb/TmdbEpisodeDto";
import {TorrentEntry} from "../pirateBay/TorrentEntry";
import {TmdbSeriesDetailDto} from "../tmdb/TmdbSeriesDetailDto";
import {TmdbMovieDetailDto} from "../tmdb/TmdbMovieDetailDto";

export class DownloadRequestDto {
  tmdbEpisode: TmdbEpisodeDto;
  seriesDetail: TmdbSeriesDetailDto;
  torrentEntry: TorrentEntry;
  movieDetail: TmdbMovieDetailDto;

  static getEpisodeString(tmdbEpisode: TmdbEpisodeDto): string {
    if (tmdbEpisode == null) {
      return "";
    }
    let episodeStr = "S";
    if (tmdbEpisode.season_number < 10) {
      episodeStr += "0";
    }
    episodeStr += tmdbEpisode.season_number.toString();
    episodeStr += "E";
    if (tmdbEpisode.episode_number < 10) {
      episodeStr += "0";
    }
    episodeStr += tmdbEpisode.episode_number.toString();
    return episodeStr;
  }

  static getDownloadTitle(downloadRequest: DownloadRequestDto) {
    if (downloadRequest.seriesDetail != null) {
      const episodeString = DownloadRequestDto.getEpisodeString(downloadRequest.tmdbEpisode)
      return downloadRequest.seriesDetail.name + " " + episodeString;
    } else if (downloadRequest.movieDetail != null){
      return downloadRequest.movieDetail.original_title;
    } else {
      return downloadRequest.torrentEntry.name;
    }
  }
}
