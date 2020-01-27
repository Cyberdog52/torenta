import {TmdbSeasonDto} from "../tmdb/TmdbSeasonDto";
import {AirStatus} from "./AirStatus";
import {DownloadStatus} from "./DownloadStatus";
import {Episode} from "./Episode";

export interface Season {
  airStatus: AirStatus;
  downloadStatus: DownloadStatus;
  episodeList: Episode[];
  tmdbSeasonDto: TmdbSeasonDto;
  seasonNumber: number;
}
