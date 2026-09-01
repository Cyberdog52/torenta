package ch.andreskonrad.torenta.preference.service;

import ch.andreskonrad.torenta.preference.dto.UserPreference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PreferenceServiceTest {

    @Mock
    private Preferences preferences;

    private PreferenceService preferenceService;

    @BeforeEach
    public void setUp() {
        preferenceService = spy(new PreferenceService());
        doReturn(preferences).when(preferenceService).getUserPreferenceRoot();
    }

    @Test
    public void saveDownloadDirectory_customFolder_stored() {
        UserPreference userPreferences = new UserPreference("C:/", "tmdb-key");
        when(preferences.get(eq("downloadDirectory"), anyString())).thenReturn("C:/");
        when(preferences.get(eq("tmdbServiceKey"), eq(null))).thenReturn("tmdb-key");

        preferenceService.save(userPreferences);

        UserPreference storedPreferences = preferenceService.loadPreferences();
        assertEquals(userPreferences, storedPreferences);
        verify(preferences).put("downloadDirectory", "C:/");
        verify(preferences).put("tmdbServiceKey", "tmdb-key");
    }

    @Test
    public void saveDownloadDirectory_null_notStored() {
        UserPreference oldPreferences = new UserPreference("C:/old", "tmdb-old");
        preferenceService.save(oldPreferences);

        UserPreference newPreferences = new UserPreference(null, null);
        preferenceService.save(newPreferences);
        when(preferences.get(eq("downloadDirectory"), anyString())).thenReturn("C:/old");
        when(preferences.get(eq("tmdbServiceKey"), eq(null))).thenReturn("tmdb-old");

        UserPreference storedPreferences = preferenceService.loadPreferences();
        assertEquals(oldPreferences, storedPreferences);
        verify(preferences, times(1)).put("downloadDirectory", "C:/old");
        verify(preferences, times(1)).put("tmdbServiceKey", "tmdb-old");
    }

}
