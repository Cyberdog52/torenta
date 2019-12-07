import {Component, Input, OnInit} from '@angular/core';
import {Episode} from "../shared/dto/tmdb/Episode";
import {SeriesDetail} from "../shared/dto/tmdb/SeriesDetail";

@Component({
  selector: 'episode',
  templateUrl: './episode.component.html',
  styleUrls: ['./episode.component.scss']
})
export class EpisodeComponent implements OnInit {

  @Input() episode : Episode;
  @Input() seriesDetail: SeriesDetail;

  constructor() { }

  ngOnInit() {
  }

  createSearchString(): string {
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
