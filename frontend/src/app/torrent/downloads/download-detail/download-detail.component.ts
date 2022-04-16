import {Component, Input, OnInit} from '@angular/core';
import {DownloadDto} from "../../../shared/dto/torrent/DownloadDto";
import {DownloadState} from "../../../shared/dto/torrent/DownloadState";
import {TmdbSeriesDetailDto} from "../../../shared/dto/tmdb/TmdbSeriesDetailDto";

@Component({
  selector: 'app-download-detail',
  templateUrl: './download-detail.component.html',
  styleUrls: ['./download-detail.component.scss']
})
export class DownloadDetailComponent implements OnInit {

  @Input()
  downloadDto: DownloadDto;

  constructor() { }

  ngOnInit() {
  }

  getProgressString(downloadDto: DownloadDto): string {
    if (downloadDto.state == DownloadState.FINISHED) {
      return "Successfully downloaded";
    }
    if (downloadDto.state == DownloadState.CANCELLED) {
      return "Cancelled";
    }
    if (downloadDto.state == DownloadState.STARTED) {
      return (downloadDto.progress * 100).toFixed(1).toString() + " %";
    }
    return downloadDto.state;
  }

  getEpisodeString(downloadDto: DownloadDto): string {
    if (downloadDto.downloadRequest.tmdbEpisodeDto == null) {
      return "";
    }
    let episodeStr = "S";
    if (downloadDto.downloadRequest.tmdbEpisodeDto.season_number < 10) {
      episodeStr += "0";
    }
    episodeStr += downloadDto.downloadRequest.tmdbEpisodeDto.season_number.toString();
    episodeStr += "E";
    if (downloadDto.downloadRequest.tmdbEpisodeDto.episode_number < 10) {
      episodeStr += "0";
    }
    episodeStr += downloadDto.downloadRequest.tmdbEpisodeDto.episode_number.toString();
    return episodeStr;
  }

  getDownloadTitle(downloadDto: DownloadDto) {
    if (downloadDto.downloadRequest.seriesDetail != null) {
      return downloadDto.downloadRequest.seriesDetail.name;
    } else {
      return downloadDto.downloadRequest.torrentEntry.name;
    }
  }

  getBackgroundImageFor(seriesDetail: TmdbSeriesDetailDto): string {
    if (seriesDetail == null || seriesDetail.backdrop_path == null) {
      return "../../assets/tvnotfound.png";
    }
    return "https://image.tmdb.org/t/p/original/" + seriesDetail.backdrop_path;
  }

  getSpeed(): string {
    if (!this.isRunning()) {
      return "0 Mbps";
    }
    if (this.downloadDto.downloadSpeedInBytesPerSecond == null || this.downloadDto.downloadSpeedInBytesPerSecond < 0.1) {
      return "0 Mbps";
    }
    const mbitsperSecond = this.downloadDto.downloadSpeedInBytesPerSecond / 125000.0;
    return mbitsperSecond.toFixed(2) + " Mbps"
  }

  getEstimatedTimeFinished(): string {
    if (!this.isRunning()) {
      return "Finished";
    }
    if (this.downloadDto.downloadSpeedInBytesPerSecond == null || this.downloadDto.downloadSpeedInBytesPerSecond < 0.1) {
      return "Never";
    }
    const bytesLeft = this.downloadDto.totalBytes * (1-this.downloadDto.progress);
    const secondsLeft = bytesLeft / this.downloadDto.downloadSpeedInBytesPerSecond;

    const date = new Date(0);
    date.setSeconds(secondsLeft);
    return date.toISOString().substr(11, 8);
  }

  isRunning(): boolean {
    return this.downloadDto.state == DownloadState.STARTED;
  }

  getPeers() {
    if (this.downloadDto.connectedPeers == null || this.downloadDto.connectedPeers === 0) {
      return "No connections";
    }
    return this.downloadDto.connectedPeers + " sources"
  }
}
