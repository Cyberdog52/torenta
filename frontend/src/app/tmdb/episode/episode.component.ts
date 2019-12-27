import {Component, Input, OnInit} from '@angular/core';
import {Episode} from "../../shared/dto/tmdb/Episode";
import {SeriesDetail} from "../../shared/dto/tmdb/SeriesDetail";
import {DirectoryService} from "../../directory/directory.service";
import {FileHierarchyDto} from "../../shared/dto/directory/FileHierarchyDto";
import {DirectoryDto} from "../../shared/dto/directory/DirectoryDto";

@Component({
  selector: 'episode',
  templateUrl: './episode.component.html',
  styleUrls: ['./episode.component.scss']
})
export class EpisodeComponent implements OnInit {

  @Input() episode : Episode;
  @Input() seriesDetail: SeriesDetail;
  @Input() alreadyDownloaded: boolean;

  constructor() { }

  ngOnInit() {
  }

}


