import {Component, OnInit} from '@angular/core';
import {TorrentService} from "../torrent.service";
import {DownloadDto} from "../../shared/dto/torrent/DownloadDto";
import {DownloadState} from "../../shared/dto/torrent/DownloadState";
import {NotificationService} from "../../shared/notification/notification.service";
import {NotificationType} from "../../shared/dto/notification/Notification";

@Component({
  selector: 'downloads',
  templateUrl: './downloads.component.html',
  styleUrls: ['./downloads.component.scss']
})
export class DownloadsComponent implements OnInit {

  downloadDtos: DownloadDto[] = [];

  constructor(private torrentService: TorrentService, private notificationService: NotificationService) {
  }

  ngOnInit() {
    this.updateDownloadDtos();
  }


  private updateDownloadDtos(): void {
    this.torrentService.getDownloadDtosObservable().subscribe(downloadDtos => {
      downloadDtos.forEach(updatedDto => {
        if (updatedDto.state === "FINISHED" && this.stateChanged(this.downloadDtos, updatedDto)) {
          this.notificationService.addNotifications({
            content: `${this.getDownloadTitle(updatedDto)} ${this.getEpisodeString(updatedDto)} successfully downloaded.`,
            type: NotificationType.INFO
          })
        }
      });
      this.downloadDtos = downloadDtos;
    });
  }

  private stateChanged(existingDownloadDtos, updatedDto) {
    let existingDto = existingDownloadDtos.find(existing => existing.id == updatedDto.id);
    return existingDto !== null ? existingDto.state != updatedDto.state : false;
  }

  downloadsLoaded() {
    return this.downloadDtos != null;
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
      return downloadDto.downloadRequest.pirateBayEntry.name;
    }
  }
}
