package ch.andreskonrad.torenta.preference.service;

import ch.andreskonrad.torenta.preference.dto.UserPreference;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.prefs.Preferences;

@Service
public class PreferenceService {

    //do not change these keys
    //by changing the following static strings users may loose preferences
    private static final String PREFERENCE_NODE_NAME = "ch/andreskonrad/torenta/preference";
    private static final String PREFERENCE_DOWNLOAD_DIRECTORY = "downloadDirectory";

    public UserPreference loadPreferences() {
        Preferences userPreferenceRoot = getUserPreferenceRoot();

        String defaultDownloadDirectoryPath = Paths.get(System.getProperty("user.home"), "Downloads").toString();
        String downloadDirectoryPath = userPreferenceRoot.get(PREFERENCE_DOWNLOAD_DIRECTORY, defaultDownloadDirectoryPath);

        return new UserPreference(downloadDirectoryPath);
    }

    public void save(UserPreference preferences) {
        Preferences userPreferenceRoot = getUserPreferenceRoot();

        if (preferences.getDownloadDirectoryPath() != null) {
            userPreferenceRoot.put(PREFERENCE_DOWNLOAD_DIRECTORY, preferences.getDownloadDirectoryPath());
        }
    }

    protected Preferences getUserPreferenceRoot() {
        return Preferences.userRoot().node(PREFERENCE_NODE_NAME);
    }
}
