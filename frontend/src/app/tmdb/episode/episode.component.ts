import {Component, Input, OnInit} from '@angular/core';
import {Episode} from "../../shared/dto/tmdb/Episode";
import {SeriesDetail} from "../../shared/dto/tmdb/SeriesDetail";

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
}
