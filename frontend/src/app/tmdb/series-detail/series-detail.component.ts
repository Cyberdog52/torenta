import {Component, Input, OnInit} from '@angular/core';
import {SearchService} from "../../search/search.service";
import {SeriesDetail} from "../../shared/dto/tmdb/SeriesDetail";
import {Season} from "../../shared/dto/tmdb/Season";
import {DirectoryService} from "../../directory/directory.service";
import {FileHierarchyDto} from "../../shared/dto/directory/FileHierarchyDto";

@Component({
  selector: 'series-detail',
  templateUrl: './series-detail.component.html',
  styleUrls: ['./series-detail.component.scss']
})
export class SeriesDetailComponent implements OnInit {

  @Input() id : number;
  public seriesDetail: SeriesDetail;
  public showSeason: Season;
  private fileHierarchy: FileHierarchyDto;

  constructor(private searchService: SearchService,
              private directoryService: DirectoryService) { }

  ngOnInit() {
    this.searchService.getTVShow(this.id).subscribe(seriesDetail => {
      this.seriesDetail = seriesDetail;
    });
    this.updateFileHierarchy();
  }

  public isLoading(): boolean {
    return this.seriesDetail == null;
  }

  private updateFileHierarchy(): void {
    this.directoryService.getFileHierarchyAsObservable().subscribe(fileHierarchy => {
      this.fileHierarchy = fileHierarchy;
    });
  }


  getBackgroundImageFor(seriesDetail: SeriesDetail): string {
    if (seriesDetail.backdrop_path == null) {
      return "../../assets/tvnotfound.png";
    }
    return "https://image.tmdb.org/t/p/original/" + seriesDetail.backdrop_path;
  }

  getRuntime(): string {
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



  isPartiallyDownloaded(season: Season): boolean {
    if (this.fileHierarchy == null) {
      return false;
    }
    const seriesFolder = this.fileHierarchy.seriesRootDirectoryDto.series.find(directoryDto=> {
      return directoryDto.name == this.seriesDetail.name;
    });
    if (seriesFolder == null) {
      return false;
    }

    const seasonFolder = seriesFolder.directories.find(directoryDto => {
      return directoryDto.name == this.getSeasonTitle(season);
    });

    return seasonFolder != null;
  }

  notAiredYet(season: Season): boolean {
    if (season.air_date == null) {
      return true;
    }
    let airDate = new Date(season.air_date);
    let currentDate = new Date();
    return airDate.valueOf() > currentDate.valueOf();
  }
}
