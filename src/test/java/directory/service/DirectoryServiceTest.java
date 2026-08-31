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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
                .thenReturn(new UserPreference(rootFolder.toAbsolutePath().toString()));
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
}