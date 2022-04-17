import {Component, Input, OnInit} from '@angular/core';
import {SearchService} from "../../search/search.service";
import {DirectoryService} from "../../directory/directory.service";
import {TmdbMovieDetailDto} from "../../shared/dto/tmdb/TmdbMovieDetailDto";
import {DirectoryDto} from "../../shared/dto/directory/DirectoryDto";

@Component({
  selector: 'movie-detail',
  templateUrl: './movie-detail.component.html',
  styleUrls: ['./movie-detail.component.scss']
})
export class MovieDetailComponent implements OnInit {

  @Input() id : number;
  movieDetail: TmdbMovieDetailDto;
  movieDirectory: DirectoryDto;

  constructor(private searchService: SearchService,
              private directoryService: DirectoryService) { }

  ngOnInit() {
    this.searchService.getMovie(this.id).subscribe(movieDetail => {
      this.movieDetail = movieDetail;
      this.getMovieDirectory();
    });

  }

  public isLoading(): boolean {
    return this.movieDetail == null;
  }

  private getMovieDirectory(): void {
    const movieTitle = this.movieDetail.title.replace(/[^a-zA-Z0-9.\- ]/, "");
    const releaseYear = +this.movieDetail.release_date.split("-")[0];
    this.directoryService.getMovieDirectory(movieTitle, releaseYear).subscribe(movieDirectory => {
      this.movieDirectory = movieDirectory;
    });
  }


  getBackgroundImageFor(movieDetail: TmdbMovieDetailDto): string {
    if (movieDetail.backdrop_path == null) {
      return "../../assets/tvnotfound.png";
    }
    return "https://image.tmdb.org/t/p/original/" + movieDetail.backdrop_path;
  }
}
