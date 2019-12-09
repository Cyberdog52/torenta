import {Component, OnDestroy, OnInit} from '@angular/core';
import {TorrentService} from "../torrent.service";
import {DownloadDto} from "../../shared/dto/torrent/DownloadDto";
import {DownloadState} from "../../shared/dto/torrent/DownloadState";

@Component({
  selector: 'downloads',
  templateUrl: './downloads.component.html',
  styleUrls: ['./downloads.component.scss']
})
export class DownloadsComponent implements OnInit, OnDestroy {

  downloadDtos: DownloadDto[];
  private intervalId: any;
  private updateIntervalInMs = 200;

  constructor(private torrentService: TorrentService) { }

  ngOnInit() {
    this.updateDownloadDtos();

    this.intervalId = setInterval(() => this.updateDownloadDtos(), this.updateIntervalInMs);
  }

  ngOnDestroy() {
    clearInterval(this.intervalId);
  }

  private updateDownloadDtos(): void {
    this.torrentService.getDownloadDtos().subscribe(downloadDtos => {
      this.downloadDtos = downloadDtos.sort((a, b) => b.startTimeInMs - a.startTimeInMs);
    });
  }

  downloadsLoaded() {
    return this.downloadDtos != null;
  }

  getProgessString(downloadDto: DownloadDto): string {
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

  getEpisodeString(downloadDto: DownloadDto) : string{
    let episodeStr = "S";
    if (downloadDto.downloadRequest.episode.season_number < 10) {
      episodeStr += "0";
    }
    episodeStr += downloadDto.downloadRequest.episode.season_number.toString();
    episodeStr += "E";
    if (downloadDto.downloadRequest.episode.episode_number < 10) {
      episodeStr += "0";
    }
    episodeStr += downloadDto.downloadRequest.episode.episode_number.toString();
    return episodeStr;
  }
}
