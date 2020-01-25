import {Component, Input, OnInit} from '@angular/core';
import {TmdbEpisodeDto} from "../../shared/dto/tmdb/TmdbEpisodeDto";
import {TmdbSeriesDetailDto} from "../../shared/dto/tmdb/TmdbSeriesDetailDto";

@Component({
  selector: 'episode',
  templateUrl: './episode.component.html',
  styleUrls: ['./episode.component.scss']
})
export class EpisodeComponent implements OnInit {

  @Input() tmdbEpisodeDto : TmdbEpisodeDto;
  @Input() seriesDetail: TmdbSeriesDetailDto;
  @Input() alreadyDownloaded: boolean;

  constructor() { }

  ngOnInit() {
  }

}


