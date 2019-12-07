import {Component, Input, OnChanges, OnInit} from '@angular/core';
import {Season} from "../shared/dto/tmdb/Season";
import {SearchService} from "../search/search.service";
import {Episode} from "../shared/dto/tmdb/Episode";
import {SeriesDetail} from "../shared/dto/tmdb/SeriesDetail";

@Component({
  selector: 'season',
  templateUrl: './season.component.html',
  styleUrls: ['./season.component.scss']
})
export class SeasonComponent implements OnInit, OnChanges {

  @Input() season : Season;
  @Input() seriesDetail: SeriesDetail;
  public showEpisode: Episode;

  public episodes: Episode[];

  constructor(private searchService: SearchService) { }

  ngOnInit() {
    this.getEpisodes();
  }

  ngOnChanges() {
    this.showEpisode = null;
    this.episodes = null;
    this.getEpisodes();
  }

  //get episodes because season does not automatically come with all episodes
  private getEpisodes() {
    this.searchService.getEpisodes(this.seriesDetail.id, this.season.season_number).subscribe(episodes => {
      this.episodes = episodes;
    });
  }

  getEpisodeButtonTitle(episode: Episode): string {
    if (episode.episode_number < 10) {
      return "E0" + episode.episode_number.toString();
    }
    return "E" + episode.episode_number.toString();
  }
}
