import {AirStatus} from "./AirStatus";
import {DownloadStatus} from "./DownloadStatus";
import {TmdbEpisodeDto} from "../tmdb/TmdbEpisodeDto";
import {DownloadDto} from "../torrent/DownloadDto";

export interface Episode {
  airStatus: AirStatus;
  downloadStatus: DownloadStatus;
  tmdbEpisodeDto: TmdbEpisodeDto;
  seasonNumber: number;
  episodeNumber: number;
  episodeString: String;
  downloadDto: DownloadDto;
}
