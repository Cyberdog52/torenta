import {Component, OnInit} from '@angular/core';
import {TorrentService} from "../torrent.service";
import {DownloadDto} from "../../shared/dto/torrent/DownloadDto";
import {DownloadState} from "../../shared/dto/torrent/DownloadState";

@Component({
  selector: 'downloads',
  templateUrl: './downloads.component.html',
  styleUrls: ['./downloads.component.scss']
})
export class DownloadsComponent implements OnInit{

  downloadDtos: DownloadDto[];

  constructor(private torrentService: TorrentService) { }

  ngOnInit() {
    this.updateDownloadDtos();
  }


  private updateDownloadDtos(): void {
    this.torrentService.getDownloadDtosObservable().subscribe(downloadDtos => {
      this.downloadDtos = downloadDtos;
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
