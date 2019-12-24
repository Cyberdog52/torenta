package ch.andreskonrad.torenta.directory.dto;

import java.util.Set;

public class SeriesRootDirectoryDto {

    private final String absolutePath;
    private final Set<DirectoryDto> series;

    public SeriesRootDirectoryDto(String absolutePath, Set<DirectoryDto> series) {
        this.absolutePath = absolutePath;
        this.series = series;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public Set<DirectoryDto> getSeries() {
        return series;
    }
}
