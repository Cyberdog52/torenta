import { TmdbEpisodeDto } from '../tmdb/TmdbEpisodeDto';
import { TorrentEntry } from '../pirateBay/TorrentEntry';
import { TmdbSeriesDetailDto } from '../tmdb/TmdbSeriesDetailDto';
import { TmdbMovieDetailDto } from '../tmdb/TmdbMovieDetailDto';

export interface DownloadRequestDto {
  tmdbEpisode: TmdbEpisodeDto | null;
  seriesDetail: TmdbSeriesDetailDto | null;
  torrentEntry: TorrentEntry;
  movieDetail: TmdbMovieDetailDto | null;
}

export function getEpisodeString(tmdbEpisode: TmdbEpisodeDto | null): string {
  if (tmdbEpisode == null) {
    return '';
  }
  const season = String(tmdbEpisode.season_number).padStart(2, '0');
  const episode = String(tmdbEpisode.episode_number).padStart(2, '0');
  return `S${season}E${episode}`;
}

export function getDownloadTitle(downloadRequest: DownloadRequestDto): string {
  if (downloadRequest.seriesDetail != null) {
    return `${downloadRequest.seriesDetail.name} ${getEpisodeString(downloadRequest.tmdbEpisode)}`;
  }
  if (downloadRequest.movieDetail != null) {
    return downloadRequest.movieDetail.original_title;
  }
  return downloadRequest.torrentEntry.name;
}

/**
 * Drops a leading franchise prefix, e.g. 'Star Wars: Andor S01E01' -> 'Andor S01E01'.
 * Only splits on the *first* colon and trims the result, so titles with more
 * than one colon (e.g. 'Marvel: Agents of S.H.I.E.L.D.: Season 1') keep
 * everything after the franchise name instead of losing it to `.pop()`.
 */
export function dropFranchisePrefix(title: string): string {
  const separatorIndex = title.indexOf(':');
  return (separatorIndex === -1 ? title : title.slice(separatorIndex + 1)).trim();
}
