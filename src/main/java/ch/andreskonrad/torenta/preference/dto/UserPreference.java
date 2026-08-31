package ch.andreskonrad.torenta.preference.dto;

import java.util.Objects;

public class UserPreference {

    private String downloadDirectoryPath;
    private String tmdbServiceKey;

    //used for jackson
    public UserPreference() {
    }

    public UserPreference(String downloadDirectoryPath, String tmdbServiceKey) {
        this.downloadDirectoryPath = downloadDirectoryPath;
        this.tmdbServiceKey = tmdbServiceKey;
    }

    public String getDownloadDirectoryPath() {
        return downloadDirectoryPath;
    }

    public String getTmdbServiceKey() {
        return tmdbServiceKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPreference that = (UserPreference) o;
        return Objects.equals(downloadDirectoryPath, that.downloadDirectoryPath)
                && Objects.equals(tmdbServiceKey, that.tmdbServiceKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(downloadDirectoryPath, tmdbServiceKey);
    }
}
