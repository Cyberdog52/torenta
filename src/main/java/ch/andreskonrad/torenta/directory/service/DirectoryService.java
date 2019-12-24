package ch.andreskonrad.torenta.directory.service;


import ch.andreskonrad.torenta.directory.dto.*;
import ch.andreskonrad.torenta.preference.service.PreferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DirectoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryService.class);
    private final static String TV_DIRECTORY_NAME = "Series";
    private final static String MOVIES_DIRECTORY_NAME = "Movies";

    private Path rootDirectoryPath;
    private Path moviesDirectoryPath;
    private Path tvDirectoryPath;

    @Autowired
    public DirectoryService(PreferenceService preferenceService) {
        String downloadDirectoryPath = preferenceService.loadPreferences().getDownloadDirectoryPath();
        rootDirectoryPath = Path.of(downloadDirectoryPath);
        createHierarchyIfNeeded();
    }

    public Path getPathForSeries(String seriesName) {
        Path seriesPath = tvDirectoryPath.resolve(seriesName);
        createDirectoryIfNeeded(seriesPath);
        return seriesPath;
    }

    public Path getPathForMovie(String movieName) {
        Path moviesPath = moviesDirectoryPath.resolve(movieName);
        createDirectoryIfNeeded(moviesPath);
        return moviesPath;
    }

    public Path getPathForSeason(String seriesName, int seasonNumber) {
        Path seriesPath = getPathForSeries(seriesName);
        Path seasonPath = seriesPath.resolve(getSeasonName(seasonNumber));
        createDirectoryIfNeeded(seasonPath);
        return seasonPath;
    }

    private String getSeasonName(int seasonNumber) {
        StringBuilder seasonNameBuilder = new StringBuilder().append("S");
        if (seasonNumber < 10) {
            seasonNameBuilder.append(0);
        }
        seasonNameBuilder.append(seasonNumber);
        return seasonNameBuilder.toString();
    }

    public FileHierarchyDto getDirectoryHierarchy() {
        MoviesRootDirectoryDto moviesRootDirectoryDto = getMoviesRootDirectoryDto();
        SeriesRootDirectoryDto seriesRootDirectoryDto = getSeriesRootDirectoryDto();

        return new FileHierarchyDto(moviesRootDirectoryDto, seriesRootDirectoryDto, rootDirectoryPath.toAbsolutePath().toString());
    }

    private MoviesRootDirectoryDto getMoviesRootDirectoryDto() {
        Set<DirectoryDto> movies = null;
        try {
            movies = Files.list(moviesDirectoryPath)
                    .filter(path -> Files.isDirectory(path))
                    .map(this::getDirectoryDto)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new MoviesRootDirectoryDto(moviesDirectoryPath.toAbsolutePath().toString(), movies);
    }

    private SeriesRootDirectoryDto getSeriesRootDirectoryDto() {
        Set<DirectoryDto> series = null;
        try {
            series = Files.list(tvDirectoryPath)
                    .filter(path -> Files.isDirectory(path))
                    .map(this::getDirectoryDto)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new SeriesRootDirectoryDto(tvDirectoryPath.toAbsolutePath().toString(), series);
    }

    private DirectoryDto getDirectoryDto(Path path) {
        try {
            String name = path.getFileName().toString();

            Set<FileDto> files = Files.list(path)
                    .filter(Files::isRegularFile)
                    .map(this::getFileDto)
                    .collect(Collectors.toSet());
            Set<DirectoryDto> directories = Files.list(path)
                    .filter(Files::isDirectory)
                    .map(this::getDirectoryDto)
                    .collect(Collectors.toSet());

            return new DirectoryDto(name, files, directories, path.toAbsolutePath().toString());
        } catch(IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private FileDto getFileDto(Path path) {
        String name = path.getFileName().toString();
        String absolutePath = path.toAbsolutePath().toString();
        return new FileDto(absolutePath, name);
    }

    private void createHierarchyIfNeeded() {
        createDirectoryIfNeeded(rootDirectoryPath);

        tvDirectoryPath = rootDirectoryPath.resolve(TV_DIRECTORY_NAME);
        createDirectoryIfNeeded(tvDirectoryPath);

        moviesDirectoryPath = rootDirectoryPath.resolve(MOVIES_DIRECTORY_NAME);
        createDirectoryIfNeeded(moviesDirectoryPath);
    }

    private boolean createDirectoryIfNeeded(Path path) {
        if (!Files.isDirectory(path)) {
            try {
                Files.createDirectory(path);
                LOGGER.info("Created directory", path);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }
}
