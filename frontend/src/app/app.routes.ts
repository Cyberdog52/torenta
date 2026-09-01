import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'search',
    title: 'Torenta – Search',
    loadComponent: () => import('./search/search.component').then((m) => m.SearchComponent),
  },
  {
    path: 'downloads',
    title: 'Torenta – Downloads',
    loadComponent: () =>
      import('./torrent/downloads/downloads.component').then((m) => m.DownloadsComponent),
  },
  {
    path: 'preferences',
    title: 'Torenta – Preferences',
    loadComponent: () =>
      import('./preference/preferences.component').then((m) => m.PreferencesComponent),
  },
  {
    path: 'recommendations',
    title: 'Torenta – Recommendations',
    loadComponent: () =>
      import('./recommendation/recommendation.component').then((m) => m.RecommendationComponent),
  },
  { path: '', redirectTo: '/search', pathMatch: 'full' },
  {
    path: '**',
    title: 'Torenta – Not found',
    loadComponent: () =>
      import('./page-not-found/page-not-found.component').then((m) => m.PageNotFoundComponent),
  },
];
