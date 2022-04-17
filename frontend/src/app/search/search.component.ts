import {Component, OnInit} from '@angular/core';
import {SearchService} from "./search.service";

@Component({
  selector: 'search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.scss']
})
export class SearchComponent implements OnInit {

  constructor(private searchService: SearchService) { }

  public searchMovieString: string;
  public searchSeriesString : string;
  public searchTorrentString: string;
  public delayedTorrentSearchString: string;
  public searchSeriesResult: TmdbSearchSeriesResultDto;
  public searchMovieResult: TmdbSearchMoviesResultDto;
  public listOfOpenendPanels: number[] = [];

  ngOnInit() {
  }

  public searchSeries() : void {
    if (this.searchSeriesString == null || this.searchSeriesString.length == 0) {
      return;
    }
    this.searchService.searchSeries(this.searchSeriesString).subscribe(searchResult => {
      this.searchSeriesResult = searchResult;
      this.listOfOpenendPanels = [];
    })
  }

  public searchMovie() : void {
    if (this.searchMovieString == null || this.searchMovieString.length == 0) {
      return;
    }
    this.searchService.searchMovies(this.searchMovieString).subscribe(searchResult => {
      this.searchMovieResult = searchResult;
      this.listOfOpenendPanels = [];
    })
  }

  public getSeriesOverviews(): TmdbSeriesOverviewDto[] {
    if (this.searchSeriesResult == null) {
      return [];
    }
    return this.searchSeriesResult.results.sort((a, b) => b.popularity - a.popularity);
  }

  getMovieOverviews(): TmdbMovieOverviewDto[] {
    if (this.searchMovieResult == null) {
      return [];
    }
    return this.searchMovieResult.results.sort((a, b) => b.popularity - a.popularity);
  }

  getImageForSeries(seriesOverview: TmdbSeriesOverviewDto): string {
    if (seriesOverview.poster_path == null) {
      return "../../assets/tvnotfound.png";
    }
    return "https://image.tmdb.org/t/p/w500/" + seriesOverview.poster_path;
  }

  getImageForMovie(tvOverview: TmdbMovieOverviewDto): string {
    if (tvOverview.poster_path == null) {
      return "../../assets/tvnotfound.png";
    }
    return "https://image.tmdb.org/t/p/w500/" + tvOverview.poster_path;
  }

  setPanelClosed(id: number) {
    const index = this.listOfOpenendPanels.indexOf(id, 0);
    if (index > -1) {
      this.listOfOpenendPanels.splice(index, 1);
    }
  }

  setPanelOpen(id: number) {
    if (this.listOfOpenendPanels.includes(id, 0)) {
      return;
    }
    this.listOfOpenendPanels.push(id);
  }
  //check if panel is open to make sure you do not load all the seriesDetail before they are opened
  //would exceed maximum of requests per 10 seconds (40)

  isPanelOpen(id: number): boolean {
    return this.listOfOpenendPanels.includes(id, 0);
  }

  public searchTorrent() {
    this.delayedTorrentSearchString = this.searchTorrentString;
  }
}
