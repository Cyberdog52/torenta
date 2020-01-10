import {Component, OnInit, ɵEMPTY_MAP} from '@angular/core';
import {SearchService} from "./search.service";
import {buildOptimizerLoader} from "@angular-devkit/build-angular/src/angular-cli-files/models/webpack-configs";
import {PirateBayService} from "../torrent/piratebay.service";

@Component({
  selector: 'search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.scss']
})
export class SearchComponent implements OnInit {

  constructor(private searchService: SearchService, private pirateBayService: PirateBayService) { }

  public searchTVString : string;
  public searchPiratebayString: string;
  public delayedPirateBaySearchString: string;
  public searchResult: SearchResult;
  public listOfOpenendPanels: number[] = [];

  ngOnInit() {
  }

  public searchTmdb() : void {
    if (this.searchTVString == null || this.searchTVString.length == 0) {
      return;
    }
    this.searchService.searchTmdb(this.searchTVString).subscribe(searchResult => {
      this.searchResult = searchResult;
      this.listOfOpenendPanels = [];
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

  public searchPiratebay() {
    this.delayedPirateBaySearchString = this.searchPiratebayString;
  }
}
