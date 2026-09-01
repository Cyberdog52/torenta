package ch.andreskonrad.torenta.directory.service;


import ch.andreskonrad.torenta.directory.dto.DirectoryDto;
import ch.andreskonrad.torenta.directory.dto.FileDto;
import ch.andreskonrad.torenta.preference.service.PreferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DirectoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryService.class);
    private final static String TV_DIRECTORY_NAME = "Series";
    private final static String MOVIES_DIRECTORY_NAME = "Movies";

    private Path rootDirectoryPath;
    private Path moviesDirectoryPath;
    private Path tvDirectoryPath;
    private PreferenceService preferenceService;

    @Autowired
    public DirectoryService(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
        this.preferenceService.setDirectoryService(this);
        setup();
    }

    public void setup() {
        String downloadDirectoryPath = this.preferenceService.loadPreferences().getDownloadDirectoryPath();
        rootDirectoryPath = Path.of(downloadDirectoryPath);
        createHierarchyIfNeeded();
    }

    public Path getPathForSeries(String seriesName) {
        return tvDirectoryPath.resolve(removeIllegalCharacters(seriesName));
    }

    private String removeIllegalCharacters(String title) {
        return title.replaceAll("[^a-zA-Z0-9\\- ]", "");
    }

    public Path getRootDirectoryPath() {
        createDirectoryIfNeeded(rootDirectoryPath);
        return rootDirectoryPath;
    }

    public Path getPathForMovie(String movieTitle, Integer releaseYear) {
        if (releaseYear != null && releaseYear > 1878) {
            return moviesDirectoryPath.resolve(removeIllegalCharacters(movieTitle) + " (" + releaseYear + ")");
        } else {
            return moviesDirectoryPath.resolve(removeIllegalCharacters(movieTitle));
        }
    }

    public Path createDirectoryToSaveSeries(String seriesName, int seasonNumber) {
        Path seriesPath = getPathForSeries(seriesName);
        createDirectoryIfNeeded(seriesPath);
        Path seasonPath = seriesPath.resolve(getSeasonName(seasonNumber));
        createDirectoryIfNeeded(seasonPath);
        return seasonPath;
    }

    public Path createDirectoryToSaveMovie(String movieName, Integer releaseYear) {
        Path moviesPath = getPathForMovie(movieName, releaseYear);
        createDirectoryIfNeeded(moviesPath);
        return moviesPath;
    }

    private String getSeasonName(int seasonNumber) {
        StringBuilder seasonNameBuilder = new StringBuilder().append("S");
        if (seasonNumber < 10) {
            seasonNameBuilder.append(0);
        }
        seasonNameBuilder.append(seasonNumber);
        return seasonNameBuilder.toString();
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
            LOGGER.info("Couldn't find directory for " + path.getFileName().toString());
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

    public List<String> getSeriesNamesModifiedWithin(Duration duration) {
        try (Stream<Path> seriesFolders = Files.list(tvDirectoryPath)) {
            Instant cutoff = Instant.now().minus(duration);
            return seriesFolders
                    .filter(Files::isDirectory)
                    .filter(seriesFolder -> wasModifiedAfter(seriesFolder, cutoff))
                    .map(seriesFolder -> seriesFolder.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.warn("Couldn't list series directories under " + tvDirectoryPath, e);
            return List.of();
        }
    }

    private boolean wasModifiedAfter(Path path, Instant cutoff) {
        // Only directory mtimes are considered, not file mtimes: torrent clients (including
        // ours) commonly preserve a downloaded file's original mtime from the torrent metadata,
        // which can be years in the past even though the file was just downloaded. A directory's
        // own mtime, on the other hand, is set by the filesystem whenever an entry is added or
        // removed inside it, so it reliably reflects recent local activity regardless of what
        // timestamp the downloaded files themselves carry.
        try (Stream<Path> directories = Files.walk(path).filter(Files::isDirectory)) {
            return directories.anyMatch(p -> isModifiedAfter(p, cutoff));
        } catch (IOException e) {
            LOGGER.warn("Couldn't determine last modified time for " + path, e);
            return false;
        }
    }

    private boolean isModifiedAfter(Path path, Instant cutoff) {
        try {
            return Files.getLastModifiedTime(path).toInstant().isAfter(cutoff);
        } catch (IOException e) {
            return false;
        }
    }

    public DirectoryDto getSeriesDirectory(String seriesTitle) {
        Path pathForSeries = getPathForSeries(seriesTitle);

        return getDirectoryDto(pathForSeries);
    }

    public DirectoryDto getMovieDirectory(String movieTitle, Integer releaseYear) {
        Path pathForSeries = getPathForMovie(movieTitle, releaseYear);

        return getDirectoryDto(pathForSeries);
    }
}
