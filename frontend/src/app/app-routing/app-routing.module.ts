import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import {PreferencesComponent} from "../preference/preferences.component";
import {DownloadsComponent} from "../torrent/downloads/downloads.component";
import {SearchComponent} from "../tmdb/search/search.component";
import {PageNotFoundComponent} from "../page-not-found/page-not-found.component";

const routes: Routes = [
  { path: 'search', component: SearchComponent },
  { path: 'downloads', component: DownloadsComponent },
  { path: 'preferences',      component: PreferencesComponent },
  { path: '',
    redirectTo: '/search',
    pathMatch: 'full'
  },
  { path: '**', component: PageNotFoundComponent }
];

@NgModule({
  imports: [
    RouterModule.forRoot(routes)
  ],
  exports: [
    RouterModule
  ],
  declarations: []
})
export class AppRoutingModule { }
