import {Component, Input, OnChanges, OnInit} from '@angular/core';
import {PirateBayService} from "./piratebay.service";
import {PirateBayEntry} from "../shared/dto/pirateBay/PirateBayEntry";

@Component({
  selector: 'torrent-suggestions',
  templateUrl: './torrent-suggestions.component.html',
  styleUrls: ['./torrent-suggestions.component.scss']
})
export class TorrentSuggestionsComponent implements OnInit, OnChanges {

  @Input() searchString: string;
  suggestions: PirateBayEntry[];
  displayedColumns: string[] = ['name', 'seeders', 'time', 'size', 'trusted'];

  constructor(private pirateBayService: PirateBayService) { }

  ngOnInit() {
  }

  ngOnChanges() {
    this.getSuggestions();
  }

  private getSuggestions() {
    this.pirateBayService.searchPirateBay(this.searchString).subscribe(suggestions => {
      console.log(this.searchString);
      console.log(suggestions);
      this.suggestions = suggestions;
    });
  }

  public isLoading(): boolean {
    return this.suggestions == null;
  }
}
