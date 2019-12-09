import { Component, OnInit } from '@angular/core';
import {UserPreference} from "../shared/dto/preference/UserPreference";
import {PreferenceService} from "./preference.service";

@Component({
  selector: 'preferences',
  templateUrl: './preferences.component.html',
  styleUrls: ['./preferences.component.scss']
})
export class PreferencesComponent implements OnInit {

  userPreferences: UserPreference;

  constructor(private preferenceService: PreferenceService) { }

  ngOnInit() {
    this.loadPreferences();
  }

  preferencesLoaded(): boolean {
    return this.userPreferences != null;
  }

  loadPreferences () {
    this.preferenceService.load().subscribe(preferences => {
      this.userPreferences = preferences;
    });
  }

  savePreferences() {
    this.preferenceService.save(this.userPreferences).subscribe(response => {
      console.log("Preferences saved.");
    });
  }
}
