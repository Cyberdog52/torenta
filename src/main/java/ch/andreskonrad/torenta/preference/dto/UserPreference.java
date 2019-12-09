package ch.andreskonrad.torenta.preference.dto;

import java.util.Objects;

public class UserPreference {

    private String downloadDirectoryPath;

    //used for jackson
    public UserPreference() {
    }

    public UserPreference(String downloadDirectoryPath) {
        this.downloadDirectoryPath = downloadDirectoryPath;
    }

    public String getDownloadDirectoryPath() {
        return downloadDirectoryPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPreference that = (UserPreference) o;
        return Objects.equals(downloadDirectoryPath, that.downloadDirectoryPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(downloadDirectoryPath);
    }
}
