import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppComponent } from './app.component';
import { SearchComponent } from './search/search.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import {MatCardModule, MatDialogModule, MatFormFieldModule, MatIconModule, MatListModule} from "@angular/material";
import {HttpClientModule} from "@angular/common/http";
import {FormsModule} from "@angular/forms";
import {MaterialModule} from "./shared/material/material.module";
import { SeriesDetailComponent } from './series-detail/series-detail.component';
import { SeasonComponent } from './season/season.component';
import { EpisodeComponent } from './episode/episode.component';
import { TorrentSuggestionsComponent } from './torrent-suggestions/torrent-suggestions.component';

@NgModule({
  declarations: [
    AppComponent,
    SearchComponent,
    SeriesDetailComponent,
    SeasonComponent,
    EpisodeComponent,
    TorrentSuggestionsComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    MaterialModule,
    MatCardModule,
    FormsModule,
    MatIconModule,
    MatDialogModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
