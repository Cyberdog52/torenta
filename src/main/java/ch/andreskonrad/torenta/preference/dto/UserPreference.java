package ch.andreskonrad.torenta.preference.dto;

import java.util.Objects;

public class UserPreference {

    private String downloadDirectoryPath;
    private String tmdbServiceKey;
    private String openAiApiKey;

    //used for jackson
    public UserPreference() {
    }

    public UserPreference(String downloadDirectoryPath, String tmdbServiceKey) {
        this(downloadDirectoryPath, tmdbServiceKey, null);
    }

    public UserPreference(String downloadDirectoryPath, String tmdbServiceKey, String openAiApiKey) {
        this.downloadDirectoryPath = downloadDirectoryPath;
        this.tmdbServiceKey = tmdbServiceKey;
        this.openAiApiKey = openAiApiKey;
    }

    public String getDownloadDirectoryPath() {
        return downloadDirectoryPath;
    }

    public String getTmdbServiceKey() {
        return tmdbServiceKey;
    }

    public String getOpenAiApiKey() {
        return openAiApiKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPreference that = (UserPreference) o;
        return Objects.equals(downloadDirectoryPath, that.downloadDirectoryPath)
                && Objects.equals(tmdbServiceKey, that.tmdbServiceKey)
                && Objects.equals(openAiApiKey, that.openAiApiKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(downloadDirectoryPath, tmdbServiceKey, openAiApiKey);
    }
}
