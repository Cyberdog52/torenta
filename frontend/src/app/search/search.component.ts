import { Component, OnInit } from '@angular/core';
import {SearchService} from "./search.service";

@Component({
  selector: 'search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.scss']
})
export class SearchComponent implements OnInit {

  constructor(private searchService: SearchService) { }

  public searchString : string;

  public searchResult: SearchResult;

  ngOnInit() {
  }

  public sendSearchRequest() : void {
    if (this.searchString == null || this.searchString.length == 0) {
      return;
    }
    this.searchService.searchTmdb(this.searchString).subscribe(searchResult => {
      this.searchResult = searchResult;
      console.log(searchResult);
    })
  }

  public getSeriesOverviews(): SeriesOverview[] {
    if (this.searchResult == null) {
      return [];
    }
    return this.searchResult.results.sort((a, b) => b.popularity - a.popularity);
  }

  getImageFor(seriesOverview: SeriesOverview): string {
    if (seriesOverview.poster_path == null) {
      return "../../assets/tvnotfound.png";
    }
    return "https://image.tmdb.org/t/p/w500/" + seriesOverview.poster_path;
  }
}
