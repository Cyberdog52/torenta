import {Component, Input, OnChanges, OnInit} from '@angular/core';
import {TmdbSeasonDto} from "../../shared/dto/tmdb/TmdbSeasonDto";
import {SearchService} from "../../search/search.service";
import {TmdbEpisodeDto} from "../../shared/dto/tmdb/TmdbEpisodeDto";
import {TmdbSeriesDetailDto} from "../../shared/dto/tmdb/TmdbSeriesDetailDto";
import {LibraryService} from "../../library/library.service";
import {TvLibrary} from "../../shared/dto/library/TvLibrary";
import {Series} from "../../shared/dto/library/Series";
import {DownloadStatus} from "../../shared/dto/library/DownloadStatus";
import {Episode} from "../../shared/dto/library/Episode";
import {Season} from "../../shared/dto/library/Season";
import {AirStatus} from "../../shared/dto/library/AirStatus";

@Component({
  selector: 'season',
  templateUrl: './season.component.html',
  styleUrls: ['./season.component.scss']
})
export class SeasonComponent implements OnInit, OnChanges {

  @Input() tmdbSeasonDto : TmdbSeasonDto;
  @Input() seriesDetail: TmdbSeriesDetailDto;
  public showEpisode: TmdbEpisodeDto;

  public episodes: TmdbEpisodeDto[];
  private tvLibrary: TvLibrary;

  constructor(private searchService: SearchService,
              private libraryService: LibraryService) { }

  ngOnInit() {
    this.getEpisodes();
    this.updateTvLibrary();
  }

  ngOnChanges() {
    this.showEpisode = null;
    this.episodes = null;
    this.getEpisodes();
  }

  //get episodes because season does not automatically come with all episodes
  private getEpisodes() {
    this.searchService.getEpisodes(this.seriesDetail.id, this.tmdbSeasonDto.season_number).subscribe(episodes => {
      this.episodes = episodes;
    });
  }

  getEpisodeButtonTitle(episode: TmdbEpisodeDto): string {
    if (episode.episode_number < 10) {
      return "E0" + episode.episode_number.toString();
    }
    return "E" + episode.episode_number.toString();
  }



  private updateTvLibrary(): void {
    this.libraryService.getTvLibraryAsObservable().subscribe(tvLibrary => {
      this.tvLibrary = tvLibrary;
    });
  }

  public isAlreadyDownloaded(episode: TmdbEpisodeDto): boolean {
    const e = this.getEpisode(episode);
    if (e == null) {
      return false;
    }
    return e.downloadStatus == DownloadStatus.DOWNLOADED;
  }


  private getSeries(episode: TmdbEpisodeDto): Series {
    if (this.tvLibrary == null) {
      return null;
    }
    return this.tvLibrary.series.find(series => {
      return series.seriesDetail.name === this.seriesDetail.name
    });
  }
  private getSeason(episode: TmdbEpisodeDto): Season {
    const series = this.getSeries(episode);
    if (series == null) {
      return null;
    }
    return series.seasonList.find(season => {
      return season.seasonNumber === episode.season_number;
    });
  }

  public getEpisode(episode: TmdbEpisodeDto): Episode {
    const season = this.getSeason(episode);
    if (season == null) {
      return null;
    }
    return season.episodeList.find(e => {
      return e.episodeNumber == episode.episode_number;
    });
  }

  getEpisodeIcon(tmdbEpisodeDto: TmdbEpisodeDto): string {
    const episode = this.getEpisode(tmdbEpisodeDto);
    if (episode == null) {
      return null;
    }

    if (episode.downloadStatus == DownloadStatus.DOWNLOADED) {
      return "done";
    }
    if (episode.downloadStatus == DownloadStatus.DOWNLOADING) {
      return "date_range"
    }
    if (episode.airStatus == AirStatus.NOT_AIRED) {
      return "arrow_downward"
    }
  }
}
