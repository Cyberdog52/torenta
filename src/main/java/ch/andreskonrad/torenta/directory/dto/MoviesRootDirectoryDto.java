package ch.andreskonrad.torenta.directory.dto;

import java.util.Set;

public class MoviesRootDirectoryDto {

    private final String absolutePath;
    private final Set<DirectoryDto> movies;

    public MoviesRootDirectoryDto(String absolutePath, Set<DirectoryDto> movies) {
        this.absolutePath = absolutePath;
        this.movies = movies;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public Set<DirectoryDto> getMovies() {
        return movies;
    }
}
