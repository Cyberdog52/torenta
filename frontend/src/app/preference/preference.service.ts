import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {UserPreference} from '../shared/dto/preference/UserPreference';

@Injectable({
  providedIn: 'root'
})
export class PreferenceService {

  private backendUrl = 'api/preference';

  constructor(private httpClient: HttpClient) {
  }

  public save(preferences: UserPreference): Observable<any> {
    const url = `${this.backendUrl}`;
    return this.httpClient.post<any>(url, preferences);
  }

  public load(): Observable<UserPreference> {
    const url = `${this.backendUrl}`;
    return this.httpClient.get<UserPreference>(url);
  }
}
