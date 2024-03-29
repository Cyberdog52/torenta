import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';

import {AppComponent} from './app.component';
import {SearchComponent} from './search/search.component';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {MatCardModule, MatDialogModule, MatIconModule} from "@angular/material";
import {HttpClientModule} from "@angular/common/http";
import {FormsModule} from "@angular/forms";
import {MaterialModule} from "./shared/material/material.module";
import {SeriesDetailComponent} from './tmdb/series-detail/series-detail.component';
import {SeasonComponent} from './tmdb/season/season.component';
import {EpisodeComponent} from './tmdb/episode/episode.component';
import {TorrentSuggestionsComponent} from './torrent/torrent-suggestions/torrent-suggestions.component';
import {PreferencesComponent} from './preference/preferences.component';
import {DownloadsComponent} from './torrent/downloads/downloads.component';
import {AppRoutingModule} from "./app-routing/app-routing.module";
import {PageNotFoundComponent} from './page-not-found/page-not-found.component';
import {ToolbarComponent} from './toolbar/toolbar.component';
import {DelayedKeyupDirective} from "./shared/delayed-keyup.directive";
import {NotificationComponent} from "./shared/notification/notification.component";
import {DownloadDetailComponent} from './torrent/downloads/download-detail/download-detail.component';
import { MovieDetailComponent } from './tmdb/movie-detail/movie-detail.component';

@NgModule({
  declarations: [
    AppComponent,
    SearchComponent,
    SeriesDetailComponent,
    SeasonComponent,
    EpisodeComponent,
    TorrentSuggestionsComponent,
    PreferencesComponent,
    DownloadsComponent,
    PageNotFoundComponent,
    ToolbarComponent,
    DelayedKeyupDirective,
    NotificationComponent,
    DownloadDetailComponent,
    MovieDetailComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    MaterialModule,
    MatCardModule,
    FormsModule,
    MatIconModule,
    MatDialogModule,
    AppRoutingModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
