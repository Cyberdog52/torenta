import {Component, OnInit, ɵEMPTY_MAP} from '@angular/core';
import {SearchService} from "./search.service";
import {buildOptimizerLoader} from "@angular-devkit/build-angular/src/angular-cli-files/models/webpack-configs";

@Component({
  selector: 'search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.scss']
})
export class SearchComponent implements OnInit {

  constructor(private searchService: SearchService) { }

  public searchString : string;

  public searchResult: SearchResult;
  public listOfOpenendPanels: number[] = [];

  ngOnInit() {
  }

  public sendSearchRequest() : void {
    if (this.searchString == null || this.searchString.length == 0) {
      return;
    }
    this.searchService.searchTmdb(this.searchString).subscribe(searchResult => {
      this.searchResult = searchResult;
      this.listOfOpenendPanels = [];
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

  setPanelClosed(id: number) {
    const index = this.listOfOpenendPanels.indexOf(id, 0);
    if (index > -1) {
      this.listOfOpenendPanels.splice(index, 1);
    }
    console.log("closed", this.listOfOpenendPanels)
  }
  setPanelOpen(id: number) {
    if (this.listOfOpenendPanels.includes(id, 0)) {
      return;
    }
    this.listOfOpenendPanels.push(id);
    console.log("openend", this.listOfOpenendPanels)
  }

  //check if panel is open to make sure you do not load all the seriesDetail before they are opened
  //would exceed maximum of requests per 10 seconds (40)
  isPanelOpen(id: number): boolean {
    return this.listOfOpenendPanels.includes(id, 0);
  }

}
