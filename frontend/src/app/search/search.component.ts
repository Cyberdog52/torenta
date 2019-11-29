import { Component, OnInit } from '@angular/core';
import {SearchService} from "./search.service";

@Component({
  selector: 'search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.css']
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

}
