import {Component, Input, OnInit} from '@angular/core';
import {SearchService} from "../search/search.service";
import {SeriesDetail} from "../shared/dto/SeriesDetail";
import {Season} from "../shared/dto/Season";

@Component({
  selector: 'series-detail',
  templateUrl: './series-detail.component.html',
  styleUrls: ['./series-detail.component.scss']
})
export class SeriesDetailComponent implements OnInit {

  @Input() id : number;
  public seriesDetail: SeriesDetail;
  public showSeason: Season;

  constructor(private searchService: SearchService) { }

  ngOnInit() {
    this.searchService.getTVShow(this.id).subscribe(seriesDetail => {
      this.seriesDetail = seriesDetail;
    });
  }

  public isLoading(): boolean {
    return this.seriesDetail == null;
  }


  getBackgroundImageFor(seriesDetail: SeriesDetail): string {
    if (seriesDetail.backdrop_path == null) {
      return "../../assets/tvnotfound.png";
    }
    return "https://image.tmdb.org/t/p/original/" + seriesDetail.backdrop_path;
  }

  getRuntime(seriesDetail: SeriesDetail): string {
    if (this.seriesDetail.episode_run_time.length > 0) {
      return this.seriesDetail.episode_run_time[0].toString();
    }
    return "?"
  }

  getSeasonTitle(season: Season): string {
    if (season.season_number < 10) {
      return "S0" + season.season_number.toString();
    }
    return "S" + season.season_number.toString();
  }

}
