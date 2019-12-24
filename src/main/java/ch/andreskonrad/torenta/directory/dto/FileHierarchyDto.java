package ch.andreskonrad.torenta.directory.dto;

public class FileHierarchyDto {

    private final MoviesRootDirectoryDto moviesRootDirectoryDto;
    private final SeriesRootDirectoryDto seriesRootDirectoryDto;
    private final String absolutePath;

    public FileHierarchyDto(MoviesRootDirectoryDto moviesRootDirectoryDto, SeriesRootDirectoryDto seriesRootDirectoryDto, String absolutePath) {
        this.moviesRootDirectoryDto = moviesRootDirectoryDto;
        this.seriesRootDirectoryDto = seriesRootDirectoryDto;
        this.absolutePath = absolutePath;
    }

    public MoviesRootDirectoryDto getMoviesRootDirectoryDto() {
        return moviesRootDirectoryDto;
    }

    public SeriesRootDirectoryDto getSeriesRootDirectoryDto() {
        return seriesRootDirectoryDto;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }
}
