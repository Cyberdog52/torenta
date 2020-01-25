import {Component, Input, OnChanges, OnInit} from '@angular/core';
import {TmdbSeasonDto} from "../../shared/dto/tmdb/TmdbSeasonDto";
import {SearchService} from "../../search/search.service";
import {TmdbEpisodeDto} from "../../shared/dto/tmdb/TmdbEpisodeDto";
import {TmdbSeriesDetailDto} from "../../shared/dto/tmdb/TmdbSeriesDetailDto";
import {DirectoryDto} from "../../shared/dto/directory/DirectoryDto";
import {FileHierarchyDto} from "../../shared/dto/directory/FileHierarchyDto";
import {DirectoryService} from "../../directory/directory.service";

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
  private fileHierarchy: FileHierarchyDto;

  constructor(private searchService: SearchService,
              private directoryService: DirectoryService) { }

  ngOnInit() {
    this.getEpisodes();
    this.updateFileHierarchy();
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


  private updateFileHierarchy(): void {
    this.directoryService.getFileHierarchyAsObservable().subscribe(fileHierarchy => {
      this.fileHierarchy = fileHierarchy;
    });
  }

  public isAlreadyDownloaded(episode: TmdbEpisodeDto): boolean {
    if (this.fileHierarchy == null) {
      return false;
    }
    const foundSeries = this.fileHierarchy.seriesRootDirectoryDto.series.find(series => {
      return series.name === this.seriesDetail.name.replace(/[^a-zA-Z0-9.\- ]/, "")
    });

    return this.seriesDirectoryHasThisEpisode(foundSeries, episode);
  }

  notAiredYet(episode: TmdbEpisodeDto): boolean {
    if (episode.air_date == null) {
      return true;
    }
    let airDate = new Date(episode.air_date);
    let currentDate = new Date();
    return airDate.valueOf() > currentDate.valueOf();
  }

  private getEpisodeTitle(episode: TmdbEpisodeDto) {
    let episodeTitle = "S";
    if (episode.season_number < 10) {
      episodeTitle += "0";
    }
    episodeTitle += episode.season_number.toString();
    episodeTitle += this.getEpisodeButtonTitle(episode);

    return episodeTitle;
  }

  private seriesDirectoryHasThisEpisode(series: DirectoryDto, episode: TmdbEpisodeDto): boolean {
    if (series == null) {
      return false;
    }

    const thisDirectoryHasEpisode = series.files.some(file => {
      return file.name.toLowerCase().includes(this.getEpisodeTitle(episode).toLowerCase());
    });

    if (thisDirectoryHasEpisode) {
      return true;
    }

    return series.directories.some(subDirectory => {
      return this.seriesDirectoryHasThisEpisode(subDirectory, episode);
    });
  }
}
