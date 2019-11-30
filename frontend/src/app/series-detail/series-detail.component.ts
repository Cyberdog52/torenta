import {Component, Input, OnInit} from '@angular/core';
import {SearchService} from "../search/search.service";
import {SeriesDetail} from "../shared/dto/SeriesDetail";

@Component({
  selector: 'series-detail',
  templateUrl: './series-detail.component.html',
  styleUrls: ['./series-detail.component.scss']
})
export class SeriesDetailComponent implements OnInit {

  @Input() id : number;
  public seriesDetail: SeriesDetail;

  constructor(private searchService: SearchService) { }

  ngOnInit() {
    this.searchService.getTVShow(this.id).subscribe(seriesDetail => {
      this.seriesDetail = seriesDetail;
    });
  }

  public isLoading(): boolean {
    return this.seriesDetail == null;
  }

}
