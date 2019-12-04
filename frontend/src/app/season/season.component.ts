import {Component, Input, OnChanges, OnInit} from '@angular/core';
import {Season} from "../shared/dto/Season";
import {SearchService} from "../search/search.service";
import {Episode} from "../shared/dto/Episode";

@Component({
  selector: 'season',
  templateUrl: './season.component.html',
  styleUrls: ['./season.component.scss']
})
export class SeasonComponent implements OnInit, OnChanges {

  @Input() season : Season;
  @Input() seriesId: number;
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
    this.searchService.getEpisodes(this.seriesId, this.season.season_number).subscribe(episodes => {
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
