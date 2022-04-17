import {Component, OnInit} from '@angular/core';
import {TorrentService} from "../torrent.service";
import {DownloadDto} from "../../shared/dto/torrent/DownloadDto";
import {NotificationService} from "../../shared/notification/notification.service";
import {NotificationType} from "../../shared/dto/notification/Notification";
import {DownloadRequestDto} from "../../shared/dto/torrent/DownloadRequestDto";

@Component({
  selector: 'downloads',
  templateUrl: './downloads.component.html',
  styleUrls: ['./downloads.component.scss']
})
export class DownloadsComponent implements OnInit {

  downloadDtos: DownloadDto[] = [];
  numberOfColumns: number;

  constructor(private torrentService: TorrentService, private notificationService: NotificationService) {
  }

  ngOnInit() {
    this.updateDownloadDtos();
    this.numberOfColumns = this.computeNumberOfColumns(window.innerWidth);
  }


  private updateDownloadDtos(): void {
    this.torrentService.getDownloadDtosObservable().subscribe(downloadDtos => {
      downloadDtos.forEach(updatedDto => {
        if (updatedDto.state === "FINISHED" && this.stateChanged(this.downloadDtos, updatedDto)) {
          this.notificationService.addNotifications({
            content: `${DownloadRequestDto.getDownloadTitle(updatedDto.downloadRequest)} successfully downloaded.`,
            type: NotificationType.INFO
          })
        }
      });
      this.downloadDtos = downloadDtos;
    });
  }

  private stateChanged(existingDownloadDtos, updatedDto) {
    let existingDto = existingDownloadDtos.find(existing => existing.id == updatedDto.id);
    if (existingDto == null || updatedDto == null) return false;
    return existingDto.state != updatedDto.state;
  }

  downloadsLoaded() {
    return this.downloadDtos != null;
  }

  handleSize(event) {
    this.numberOfColumns = this.computeNumberOfColumns(event.target.innerWidth);
  }

  private computeNumberOfColumns(windowWidth: number): number {
    return windowWidth / 600;
  }




}
