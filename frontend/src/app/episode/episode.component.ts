import {Component, Input, OnInit} from '@angular/core';
import {Season} from "../shared/dto/Season";
import {Episode} from "../shared/dto/Episode";

@Component({
  selector: 'episode',
  templateUrl: './episode.component.html',
  styleUrls: ['./episode.component.scss']
})
export class EpisodeComponent implements OnInit {

  @Input() episode : Episode;

  constructor() { }

  ngOnInit() {
  }

}
