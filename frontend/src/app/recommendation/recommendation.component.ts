import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { RecommendationService, DEFAULT_RECOMMENDATION_DAYS } from './recommendation.service';
import { SeriesTorrentsComponent } from './series-torrents/series-torrents.component';
import { safeValue } from '../shared/resource';
import { backdropUrl } from '../shared/tmdb-images';
import { NotificationService } from '../shared/notification/notification.service';
import { NotificationType } from '../shared/dto/notification/Notification';

@Component({
  selector: 'app-recommendation',
  imports: [
    RouterLink,
    MatCardModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    SeriesTorrentsComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './recommendation.component.scss',
  templateUrl: './recommendation.component.html',
})
export class RecommendationComponent {
  private readonly recommendationService = inject(RecommendationService);
  private readonly notificationService = inject(NotificationService);

  /**
   * How far back (in days) a series folder must have been touched to still
   * be considered for recommendations, keeping the scan fast for large
   * libraries. `0` means "no filter": scan the whole library.
   */
  protected readonly days = signal(DEFAULT_RECOMMENDATION_DAYS);

  private readonly recommendationsResource = this.recommendationService.recommendationsResource(
    this.days,
  );

  protected readonly result = computed(() => safeValue(this.recommendationsResource));

  protected readonly recommendations = computed(() => this.result()?.recommendations ?? []);

  protected readonly seriesConsidered = computed(() => this.result()?.seriesConsidered ?? 0);

  protected readonly unresolvedSeriesNames = computed(
    () => this.result()?.unresolvedSeriesNames ?? [],
  );

  protected readonly backdropUrl = backdropUrl;

  constructor() {
    effect(() => {
      if (this.recommendationsResource.error()) {
        this.notificationService.notify({
          content: 'Recommendations could not be loaded.',
          // Shown as a toast: recommendations are a convenience feature, not
          // critical enough to warrant the persistent error banner.
          type: NotificationType.INFO,
        });
      }
    });
  }

  protected setDays(input: HTMLInputElement): void {
    const value = Number(input.value);
    if (Number.isFinite(value) && value >= 0) {
      this.days.set(Math.trunc(value));
    }
  }
}
