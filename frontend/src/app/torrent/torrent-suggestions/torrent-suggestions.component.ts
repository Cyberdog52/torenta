import {Component, Input, OnChanges, OnInit} from '@angular/core';
import {TorrentEntry} from "../../shared/dto/pirateBay/TorrentEntry";
import {TorrentService} from "../torrent.service";
import {DownloadRequestDto} from "../../shared/dto/torrent/DownloadRequestDto";
import {TmdbEpisodeDto} from "../../shared/dto/tmdb/TmdbEpisodeDto";
import {TmdbSeriesDetailDto} from "../../shared/dto/tmdb/TmdbSeriesDetailDto";
import {DownloadDto} from "../../shared/dto/torrent/DownloadDto";
import {NotificationService} from "../../shared/notification/notification.service";
import {NotificationType} from "../../shared/dto/notification/Notification";
import {TmdbMovieDetailDto} from "../../shared/dto/tmdb/TmdbMovieDetailDto";

@Component({
  selector: 'torrent-suggestions',
  templateUrl: './torrent-suggestions.component.html',
  styleUrls: ['./torrent-suggestions.component.scss']
})
export class TorrentSuggestionsComponent implements OnInit, OnChanges {

  @Input() seriesDetail?: TmdbSeriesDetailDto;
  @Input() tmdbEpisodeDto?: TmdbEpisodeDto;
  @Input() movieDetail?: TmdbMovieDetailDto;
  @Input() searchString?: string;
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
    const downloadRequest: DownloadRequestDto = {
      "tmdbEpisode": this.tmdbEpisodeDto,
      "seriesDetail": this.seriesDetail,
      "torrentEntry": torrentEntry,
      "movieDetail": this.movieDetail
    };
    console.log(downloadRequest);
    this.torrentService.startTorrent(downloadRequest).subscribe(response => {
      console.log("Torrent started for downloadRequest ", downloadRequest);
      this.notificationService.addNotifications({
        content: `Started downloading ${DownloadRequestDto.getDownloadTitle(downloadRequest)}`,
        type: NotificationType.INFO
      })
    });
  }

  createSearchString(): string {
    let searchString = this.seriesDetail ? this.seriesDetail.name + " " + DownloadRequestDto.getEpisodeString(this.tmdbEpisodeDto) : this.movieDetail.title;
    return searchString.split(':').pop(); // change 'Star Wars: Andor S01E01' to 'Andor S01E01'
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
