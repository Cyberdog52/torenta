import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { PreferenceService } from './preference.service';
import { UserPreference } from '../shared/dto/preference/UserPreference';
import { NotificationService } from '../shared/notification/notification.service';
import { NotificationType } from '../shared/dto/notification/Notification';

@Component({
  selector: 'app-preferences',
  imports: [FormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './preferences.component.scss',
  templateUrl: './preferences.component.html',
})
export class PreferencesComponent {
  private readonly preferenceService = inject(PreferenceService);
  private readonly notificationService = inject(NotificationService);

  protected readonly userPreferences = signal<UserPreference | null>(null);

  constructor() {
    this.preferenceService.load().subscribe({
      next: (preferences) => this.userPreferences.set(preferences),
      error: () =>
        this.notificationService.notify({
          content: 'Preferences could not be loaded.',
          type: NotificationType.ERROR,
        }),
    });
  }

  protected savePreferences(): void {
    const preferences = this.userPreferences();
    if (preferences == null) {
      return;
    }
    this.preferenceService.save(preferences).subscribe({
      next: () =>
        this.notificationService.notify({
          content: 'Preferences saved.',
          type: NotificationType.INFO,
        }),
      error: () =>
        this.notificationService.notify({
          content: 'Preferences could not be saved.',
          type: NotificationType.ERROR,
        }),
    });
  }

  protected setDownloadDirectoryPath(downloadDirectoryPath: string): void {
    this.userPreferences.update((preferences) =>
      preferences == null ? preferences : { ...preferences, downloadDirectoryPath },
    );
  }
}
