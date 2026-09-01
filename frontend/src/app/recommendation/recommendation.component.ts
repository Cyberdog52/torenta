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
import { RecommendationService, DEFAULT_RECOMMENDATION_WEEKS } from './recommendation.service';
import { safeValue } from '../shared/resource';
import { posterUrl } from '../shared/tmdb-images';
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
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './recommendation.component.scss',
  templateUrl: './recommendation.component.html',
})
export class RecommendationComponent {
  private readonly recommendationService = inject(RecommendationService);
  private readonly notificationService = inject(NotificationService);

  /**
   * How far back (in weeks) a series folder must have been touched to still
   * be considered for recommendations, keeping the scan fast for large
   * libraries. Defaults to 2 weeks; editable in the UI.
   */
  protected readonly weeks = signal(DEFAULT_RECOMMENDATION_WEEKS);

  private readonly recommendationsResource = this.recommendationService.recommendationsResource(
    this.weeks,
  );

  protected readonly recommendations = computed(
    () => safeValue(this.recommendationsResource) ?? [],
  );

  protected readonly posterUrl = posterUrl;

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

  protected setWeeks(input: HTMLInputElement): void {
    const value = Number(input.value);
    if (Number.isFinite(value) && value > 0) {
      this.weeks.set(Math.trunc(value));
    }
  }
}
