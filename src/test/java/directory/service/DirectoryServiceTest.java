package directory.service;

import ch.andreskonrad.torenta.directory.dto.DirectoryDto;
import ch.andreskonrad.torenta.directory.dto.FileDto;
import ch.andreskonrad.torenta.directory.service.DirectoryService;
import ch.andreskonrad.torenta.preference.dto.UserPreference;
import ch.andreskonrad.torenta.preference.service.PreferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DirectoryServiceTest {

    @Mock
    private PreferenceService preferenceService;

    private DirectoryService directoryService;

    @TempDir
    Path rootFolder;

    @BeforeEach
    public void setUp() {
        when(preferenceService.loadPreferences())
                .thenReturn(new UserPreference(rootFolder.toAbsolutePath().toString(), null));
        directoryService = new DirectoryService(preferenceService);
        verify(preferenceService).setDirectoryService(directoryService);
    }

    @Test
    public void initalizeDirectoryService_moviesDirectoryCreated() {
        Path moviesPath = rootFolder.resolve("Movies");

        assertTrue(Files.exists(moviesPath));
        assertTrue(Files.isDirectory(moviesPath));
    }

    @Test
    public void initalizeDirectoryService_seriesDirectoryCreated() {
        Path seriesPath = rootFolder.resolve("Series");

        assertTrue(Files.exists(seriesPath));
        assertTrue(Files.isDirectory(seriesPath));
    }

    @Test
    public void createDirectoryToSaveMovie_createsMovieDirectory() {
        Path moviePath = directoryService.createDirectoryToSaveMovie("Star Wars", 1977);

        assertTrue(Files.exists(moviePath));
        assertTrue(Files.isDirectory(moviePath));
        String parentName = moviePath.getParent().getFileName().toString();
        assertEquals("Movies", parentName);
        assertEquals("Star Wars (1977)", moviePath.getFileName().toString());
    }

    @Test
    public void getSeriesPath_doesNotCreateDirectory() {
        Path seriesPath = directoryService.getPathForSeries("Mandalorian");

        assertFalse(Files.exists(seriesPath));
    }

    @Test
    public void getSeasonPath_season1_createsSeasonDirectory() {
        Path seasonPath = directoryService.createDirectoryToSaveSeries("Mandalorian", 1);

        assertTrue(Files.exists(seasonPath));
        assertTrue(Files.isDirectory(seasonPath));
        assertEquals("S01", seasonPath.getFileName().toString());
        String parentName = seasonPath.getParent().getFileName().toString();
        assertEquals("Mandalorian", parentName);
    }

    @Test
    public void getSeasonPath_season10_createsSeasonDirectory() {
        Path seasonPath = directoryService.createDirectoryToSaveSeries("Mandalorian", 10);

        assertTrue(Files.exists(seasonPath));
        assertTrue(Files.isDirectory(seasonPath));
        assertEquals("S10", seasonPath.getFileName().toString());
        String parentName = seasonPath.getParent().getFileName().toString();
        assertEquals("Mandalorian", parentName);
    }

    @Test
    public void getPaths_punctuationAndSeparatorsAreSanitizedAndContainedInRoot() {
        Path seriesPath = directoryService.getPathForSeries("../Star: Trek/Discovery\\?");
        Path moviePath = directoryService.getPathForMovie("../Movie/Name:?", null);
        Path normalizedRoot = rootFolder.toAbsolutePath().normalize();

        assertEquals("Star TrekDiscovery", seriesPath.getFileName().toString());
        assertEquals("MovieName", moviePath.getFileName().toString());
        assertTrue(seriesPath.toAbsolutePath().normalize().startsWith(normalizedRoot));
        assertTrue(moviePath.toAbsolutePath().normalize().startsWith(normalizedRoot));
    }

    @Test
    public void getPathForMovie_addsOnlyYearsAfterFirstFeatureFilm() {
        assertEquals(
                "Metropolis",
                directoryService.getPathForMovie("Metropolis", null).getFileName().toString());
        assertEquals(
                "Metropolis",
                directoryService.getPathForMovie("Metropolis", 1878).getFileName().toString());
        assertEquals(
                "Metropolis (1879)",
                directoryService.getPathForMovie("Metropolis", 1879).getFileName().toString());
    }

    @Test
    public void setup_repeatedCallsAreIdempotentAndPreserveContents() throws IOException {
        Path existingMovie = Files.createDirectory(rootFolder.resolve("Movies").resolve("Existing"));

        directoryService.setup();
        directoryService.setup();

        assertTrue(Files.isDirectory(rootFolder.resolve("Movies")));
        assertTrue(Files.isDirectory(rootFolder.resolve("Series")));
        assertTrue(Files.isDirectory(existingMovie));
        verify(preferenceService, times(3)).loadPreferences();
    }

    @Test
    public void setup_missingHierarchyDirectoriesAreRecreated() throws IOException {
        Files.delete(rootFolder.resolve("Movies"));
        Files.delete(rootFolder.resolve("Series"));

        directoryService.setup();

        assertTrue(Files.isDirectory(rootFolder.resolve("Movies")));
        assertTrue(Files.isDirectory(rootFolder.resolve("Series")));
    }

    @Test
    public void getSeriesDirectory_missingDirectoryReturnsNull() {
        assertNull(directoryService.getSeriesDirectory("Missing"));
    }

    @Test
    public void getFileHierarchy_newSeries_episodeFound() throws IOException {
        Path seriesPath = rootFolder.resolve("Series");
        Path mandanlorianPath = Files.createDirectory(seriesPath.resolve("Mandalorian"));
        Path season01 = Files.createDirectory(mandanlorianPath.resolve("S01"));
        Path episodeFilePath = Files.createFile(season01.resolve("empty-episode-S01E01.mp4"));

        DirectoryDto mandalorian = directoryService.getSeriesDirectory("Mandalorian");

        assertNotNull(mandalorian);
        assertEquals("Mandalorian", mandalorian.getName());
        assertEquals(mandanlorianPath.toString(), mandalorian.getAbsolutePath());

        DirectoryDto season = mandalorian.getDirectories().stream().findFirst().orElse(null);
        assertNotNull(season);
        assertEquals("S01", season.getName());
        assertEquals(season01.toString(), season.getAbsolutePath());

        FileDto episode = season.getFiles().stream().findFirst().orElse(null);
        assertNotNull(episode);
        assertEquals("empty-episode-S01E01.mp4", episode.getName());
        assertEquals(episodeFilePath.toString(), episode.getAbsolutePath());

        assertEquals(1, season.getFiles().size());
        assertEquals(0, season.getDirectories().size());
        assertEquals(1, mandalorian.getDirectories().size());
        assertEquals(0, mandalorian.getFiles().size());
    }

    @Test
    public void getFileHierarchy_multipleNestedEntriesAreReturned() throws IOException {
        Path seriesPath = Files.createDirectory(rootFolder.resolve("Series").resolve("Mandalorian"));
        Files.createFile(seriesPath.resolve("poster.jpg"));
        Path seasonOne = Files.createDirectory(seriesPath.resolve("S01"));
        Path seasonTwo = Files.createDirectory(seriesPath.resolve("S02"));
        Files.createFile(seasonOne.resolve("episode-S01E01.mp4"));
        Files.createFile(seasonOne.resolve("episode-S01E02.mp4"));
        Path extras = Files.createDirectory(seasonTwo.resolve("Extras"));
        Files.createFile(extras.resolve("trailer.mp4"));

        DirectoryDto series = directoryService.getSeriesDirectory("Mandalorian");

        assertNotNull(series);
        assertEquals(Set.of("poster.jpg"), namesOfFiles(series));
        assertEquals(Set.of("S01", "S02"), namesOfDirectories(series));

        DirectoryDto firstSeason = directoryNamed(series, "S01");
        assertEquals(Set.of("episode-S01E01.mp4", "episode-S01E02.mp4"), namesOfFiles(firstSeason));

        DirectoryDto secondSeason = directoryNamed(series, "S02");
        DirectoryDto extrasDirectory = directoryNamed(secondSeason, "Extras");
        assertEquals(Set.of("trailer.mp4"), namesOfFiles(extrasDirectory));

        List<DirectoryDto> hierarchy = List.of(series, firstSeason, secondSeason, extrasDirectory);
        assertTrue(hierarchy.stream()
                .map(DirectoryDto::getAbsolutePath)
                .map(Path::of)
                .allMatch(path -> path.normalize().startsWith(rootFolder.toAbsolutePath().normalize())));
    }

    @Test
    public void getSeriesNamesModifiedWithin_recentlyModifiedSeries_isIncluded() throws IOException {
        Path seriesPath = rootFolder.resolve("Series").resolve("Recent Show");
        Files.createDirectory(seriesPath);

        List<String> seriesNames = directoryService.getSeriesNamesModifiedWithin(Duration.ofDays(14));

        assertEquals(List.of("Recent Show"), seriesNames);
    }

    @Test
    public void getSeriesNamesModifiedWithin_staleSeries_isExcluded() throws IOException {
        Path seriesPath = rootFolder.resolve("Series").resolve("Old Show");
        Files.createDirectory(seriesPath);
        setLastModifiedRecursively(seriesPath, Instant.now().minus(Duration.ofDays(30)));

        List<String> seriesNames = directoryService.getSeriesNamesModifiedWithin(Duration.ofDays(14));

        assertTrue(seriesNames.isEmpty());
    }

    @Test
    public void getSeriesNamesModifiedWithin_freshFileWithOldMtimeInStaleFolder_isExcluded() throws IOException {
        // Torrent clients (including ours) commonly preserve a downloaded file's original mtime
        // from the torrent metadata, which can be years old even though it was just downloaded.
        // A stale file mtime alone must not make an otherwise untouched series folder count as
        // recently modified.
        Path seriesPath = rootFolder.resolve("Series").resolve("Old Show");
        Path seasonPath = Files.createDirectories(seriesPath.resolve("S03"));
        Path oldEpisode = Files.createFile(seasonPath.resolve("episode-S03E01.mp4"));
        Instant longAgo = Instant.now().minus(Duration.ofDays(365 * 5));
        setLastModifiedRecursively(seriesPath, longAgo);
        Files.setLastModifiedTime(oldEpisode, FileTime.from(longAgo));

        List<String> seriesNames = directoryService.getSeriesNamesModifiedWithin(Duration.ofDays(14));

        assertTrue(seriesNames.isEmpty());
    }

    @Test
    public void getSeriesNamesModifiedWithin_newlyCreatedSeasonFolderWithOldFileMtimes_isIncluded()
            throws IOException {
        // Simulates a fresh download whose extracted file preserves an old mtime from the
        // torrent metadata: the season folder itself was just created by the app, so it (not the
        // file inside it) must be what makes the series count as recently modified.
        Path seriesPath = rootFolder.resolve("Series").resolve("Old Show");
        Files.createDirectory(seriesPath);
        setLastModifiedRecursively(seriesPath, Instant.now().minus(Duration.ofDays(30)));

        Path seasonPath = Files.createDirectory(seriesPath.resolve("S03"));
        Path newEpisode = Files.createFile(seasonPath.resolve("episode-S03E01.mp4"));
        Files.setLastModifiedTime(newEpisode, FileTime.from(Instant.now().minus(Duration.ofDays(365 * 5))));

        List<String> seriesNames = directoryService.getSeriesNamesModifiedWithin(Duration.ofDays(14));

        assertEquals(List.of("Old Show"), seriesNames);
    }

    private static void setLastModifiedRecursively(Path root, Instant instant) throws IOException {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.collect(Collectors.toList())) {
                Files.setLastModifiedTime(path, FileTime.from(instant));
            }
        }
    }

    private static DirectoryDto directoryNamed(DirectoryDto parent, String name) {
        return parent.getDirectories().stream()
                .filter(directory -> directory.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static Set<String> namesOfDirectories(DirectoryDto directory) {
        return directory.getDirectories().stream()
                .map(DirectoryDto::getName)
                .collect(Collectors.toSet());
    }

    private static Set<String> namesOfFiles(DirectoryDto directory) {
        return directory.getFiles().stream()
                .map(FileDto::getName)
                .collect(Collectors.toSet());
    }
}