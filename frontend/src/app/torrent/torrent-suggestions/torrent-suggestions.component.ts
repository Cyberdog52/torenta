import {Component, Input, OnChanges, OnInit} from '@angular/core';
import {TorrentEntry} from "../../shared/dto/pirateBay/TorrentEntry";
import {TorrentService} from "../torrent.service";
import {DownloadRequest} from "../../shared/dto/torrent/DownloadRequest";
import {TmdbEpisodeDto} from "../../shared/dto/tmdb/TmdbEpisodeDto";
import {TmdbSeriesDetailDto} from "../../shared/dto/tmdb/TmdbSeriesDetailDto";
import {DownloadDto} from "../../shared/dto/torrent/DownloadDto";
import {NotificationService} from "../../shared/notification/notification.service";
import {NotificationType} from "../../shared/dto/notification/Notification";

@Component({
  selector: 'torrent-suggestions',
  templateUrl: './torrent-suggestions.component.html',
  styleUrls: ['./torrent-suggestions.component.scss']
})
export class TorrentSuggestionsComponent implements OnInit, OnChanges {

  @Input() seriesDetail: TmdbSeriesDetailDto;
  @Input() tmdbEpisodeDto: TmdbEpisodeDto;
  @Input() searchString: string;
  suggestions: TorrentEntry[];
  displayedColumns: string[] = ['name', 'seeders', 'time', 'size', 'trusted', 'startDownload'];
  downloadDtos: DownloadDto[] = [];

  constructor(private torrentService: TorrentService,
              private notificationService: NotificationService) {
  }

  ngOnInit() {
    this.updateDownloadDtos();
  }

  private updateDownloadDtos(): void {
    this.torrentService.getDownloadDtosObservable().subscribe(downloadDtos => {
      this.downloadDtos = downloadDtos;
    });
  }

  ngOnChanges() {
    this.suggestions = null;
    this.getSuggestions();
  }

  private getSuggestions() {
    const selectedSearchString = this.searchString ? this.searchString : this.createSearchString();
    console.log(selectedSearchString)
    this.torrentService.searchTorrent(selectedSearchString).subscribe(suggestions => {
      //only save new suggestions if the user did not already search for another string
      if (this.searchString == null || selectedSearchString == this.searchString) {
        this.suggestions = suggestions;
      }
    });
  }

  public isLoading(): boolean {
    return this.suggestions == null;
  }

  startDownload(torrentEntry: TorrentEntry) {
    const downloadRequest: DownloadRequest = {
      "tmdbEpisodeDto": this.tmdbEpisodeDto,
      "seriesDetail": this.seriesDetail,
      "torrentEntry": torrentEntry

    };
    this.torrentService.startTorrent(downloadRequest).subscribe(response => {
      console.log("Torrent started for downloadRequest ", downloadRequest);
      this.notificationService.addNotifications({
        content: `Started downloading ${downloadRequest.seriesDetail.name} S${downloadRequest.tmdbEpisodeDto.season_number}E${downloadRequest.tmdbEpisodeDto.episode_number}`,
        type: NotificationType.INFO
      })
    });
  }

  createSearchString(): string {
    return this.seriesDetail.name + " " + this.getEpisodeString();
  }

  getEpisodeString(): string {
    let episodeStr = "S";
    if (this.tmdbEpisodeDto.season_number < 10) {
      episodeStr += "0";
    }
    episodeStr += this.tmdbEpisodeDto.season_number.toString();
    episodeStr += "E";
    if (this.tmdbEpisodeDto.episode_number < 10) {
      episodeStr += "0";
    }
    episodeStr += this.tmdbEpisodeDto.episode_number.toString();
    return episodeStr;
  }

  hasNotStartedDownload(pirateBayEntry: TorrentEntry): boolean {
    return !this.hasStartedDownload(pirateBayEntry) && !this.hasFullyDownloaded(pirateBayEntry);
  }

  hasStartedDownload(pirateBayEntry: TorrentEntry): boolean {
    return this.downloadDtos.some(downloadDto => {
      return downloadDto.downloadRequest.torrentEntry.magnetLink == pirateBayEntry.magnetLink
    })
  }

  hasFullyDownloaded(pirateBayEntry: TorrentEntry): boolean {
    //TODO: once we get the information from the file system
    return false;
  }
}
