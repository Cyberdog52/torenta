import {Component, Input, OnChanges, OnInit} from '@angular/core';
import {PirateBayService} from "../piratebay.service";
import {PirateBayEntry} from "../../shared/dto/pirateBay/PirateBayEntry";
import {TorrentService} from "../torrent.service";
import {DownloadRequest} from "../../shared/dto/torrent/DownloadRequest";
import {Episode} from "../../shared/dto/tmdb/Episode";
import {SeriesDetail} from "../../shared/dto/tmdb/SeriesDetail";

@Component({
  selector: 'torrent-suggestions',
  templateUrl: './torrent-suggestions.component.html',
  styleUrls: ['./torrent-suggestions.component.scss']
})
export class TorrentSuggestionsComponent implements OnInit, OnChanges {

  @Input() seriesDetail: SeriesDetail;
  @Input() episode: Episode;
  suggestions: PirateBayEntry[];
  displayedColumns: string[] = ['name', 'seeders', 'time', 'size', 'trusted', 'startDownload'];

  constructor(private pirateBayService: PirateBayService,
              private torrentService: TorrentService) { }

  ngOnInit() {
  }

  ngOnChanges() {
    this.getSuggestions();
  }

  private getSuggestions() {
    this.pirateBayService.searchPirateBay(this.getSearchString()).subscribe(suggestions => {
      this.suggestions = suggestions;
    });
  }

  public isLoading(): boolean {
    return this.suggestions == null;
  }

  startDownload(pirateBayEntry: PirateBayEntry) {
    const downloadRequest: DownloadRequest = {
      "episode": this.episode,
      "seriesDetail": this.seriesDetail,
      "pirateBayEntry": pirateBayEntry

    };
    this.torrentService.startTorrent(downloadRequest).subscribe(response => {
      console.log("Torrent started for downloadRequest ", downloadRequest);
    });
  }

  getSearchString(): string {
    return this.seriesDetail.name + " " + this.getEpisodeString();
  }

  getEpisodeString() : string{
    let episodeStr = "S";
    if (this.episode.season_number < 10) {
      episodeStr += "0";
    }
    episodeStr += this.episode.season_number.toString();
    episodeStr += "E";
    if (this.episode.episode_number < 10) {
      episodeStr += "0";
    }
    episodeStr += this.episode.episode_number.toString();
    return episodeStr;
  }
}
