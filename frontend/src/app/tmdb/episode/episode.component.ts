import {Component, Input, OnInit} from '@angular/core';
import {TmdbEpisodeDto} from "../../shared/dto/tmdb/TmdbEpisodeDto";
import {TmdbSeriesDetailDto} from "../../shared/dto/tmdb/TmdbSeriesDetailDto";
import {DownloadStatus} from "../../shared/dto/library/DownloadStatus";

@Component({
  selector: 'episode',
  templateUrl: './episode.component.html',
  styleUrls: ['./episode.component.scss']
})
export class EpisodeComponent implements OnInit {

  @Input() tmdbEpisodeDto : TmdbEpisodeDto;
  @Input() seriesDetail: TmdbSeriesDetailDto;
  @Input() downloadStatus: DownloadStatus;

  constructor() { }

  ngOnInit() {
  }

  getDownloadText(): string {
    switch (this.downloadStatus) {
      case DownloadStatus.DOWNLOADING: return "Downloading";
      case DownloadStatus.DOWNLOADED: return "Downloaded";
      default: return "";
    }
  }

  hasDownloadStatus() {
    return this.downloadStatus!=null;
  }
}


