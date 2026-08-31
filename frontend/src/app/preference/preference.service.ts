import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserPreference } from '../shared/dto/preference/UserPreference';

const BACKEND_URL = 'api/preference';

@Injectable({ providedIn: 'root' })
export class PreferenceService {
  private readonly httpClient = inject(HttpClient);

  save(preferences: UserPreference): Observable<void> {
    return this.httpClient.post<void>(BACKEND_URL, preferences);
  }

  load(): Observable<UserPreference> {
    return this.httpClient.get<UserPreference>(BACKEND_URL);
  }
}
