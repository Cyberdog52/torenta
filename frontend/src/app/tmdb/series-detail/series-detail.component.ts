import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { SearchService } from '../../search/search.service';
import { DirectoryService } from '../../directory/directory.service';
import { TmdbSeasonDto } from '../../shared/dto/tmdb/TmdbSeasonDto';
import { SeasonComponent } from '../season/season.component';
import { backdropUrl } from '../../shared/tmdb-images';
import { safeValue } from '../../shared/resource';

@Component({
  selector: 'app-series-detail',
  imports: [
    MatIconModule,
    MatButtonModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    SeasonComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './series-detail.component.scss',
  templateUrl: './series-detail.component.html',
})
export class SeriesDetailComponent {
  private readonly searchService = inject(SearchService);
  private readonly directoryService = inject(DirectoryService);

  readonly id = input.required<number>();

  private readonly series = this.searchService.seriesDetailResource(this.id);

  protected readonly seriesDetail = this.series.value;
  protected readonly isLoading = this.series.isLoading;

  private readonly seriesDirectory = this.directoryService.seriesDirectoryResource(
    computed(() => this.seriesDetail()?.name.replace(/[^a-zA-Z0-9.\- ]/, '')),
  );

  protected readonly showSeason = signal<TmdbSeasonDto | null>(null);

  protected readonly backdropUrl = backdropUrl;

  protected readonly runtime = computed(() => {
    const runtimes = this.seriesDetail()?.episode_run_time ?? [];
    return runtimes.length > 0 ? String(runtimes[0]) : '?';
  });

  protected seasonTitle(season: TmdbSeasonDto): string {
    return `S${String(season.season_number).padStart(2, '0')}`;
  }

  protected isPartiallyDownloaded(season: TmdbSeasonDto): boolean {
    const directories = safeValue(this.seriesDirectory)?.directories;
    return directories?.some((directory) => directory.name === this.seasonTitle(season)) ?? false;
  }

  protected notAiredYet(season: TmdbSeasonDto): boolean {
    if (season.air_date == null) {
      return true;
    }
    return new Date(season.air_date).valueOf() > Date.now();
  }
}
