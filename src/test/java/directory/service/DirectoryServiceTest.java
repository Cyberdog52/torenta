package directory.service;

import ch.andreskonrad.torenta.directory.dto.DirectoryDto;
import ch.andreskonrad.torenta.directory.dto.FileDto;
import ch.andreskonrad.torenta.directory.service.DirectoryService;
import ch.andreskonrad.torenta.preference.dto.UserPreference;
import ch.andreskonrad.torenta.preference.service.PreferenceServiceMock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = PreferenceServiceMock.class)
@EnableConfigurationProperties
public class DirectoryServiceTest {

    @Autowired
    private PreferenceServiceMock preferenceServiceMock;

    private DirectoryService directoryService;

    @Rule
    public TemporaryFolder rootFolder = new TemporaryFolder();

    @Before
    public void setUp() {
        preferenceServiceMock.save(new UserPreference(rootFolder.getRoot().getAbsolutePath().toString()));
        directoryService = new DirectoryService(preferenceServiceMock);
    }

    @Test
    public void initalizeDirectoryService_moviesDirectoryCreated() {
        Path moviesPath = rootFolder.getRoot().toPath().resolve("Movies");

        assertTrue(Files.exists(moviesPath));
        assertTrue(Files.isDirectory(moviesPath));
    }

    @Test
    public void initalizeDirectoryService_seriesDirectoryCreated() {
        Path seriesPath = rootFolder.getRoot().toPath().resolve("Series");

        assertTrue(Files.exists(seriesPath));
        assertTrue(Files.isDirectory(seriesPath));
    }

    @Test
    public void getMoviePath_createsMovieDirectory() {
        Path moviePath = directoryService.getPathForMovie("Star Wars");

        assertTrue(Files.exists(moviePath));
        assertTrue(Files.isDirectory(moviePath));
        String parentName = moviePath.getParent().getFileName().toString();
        assertEquals("Movies", parentName);
    }

    @Test
    public void getSeriesPath_doesNotCreateDirectory() {
        Path seriesPath = directoryService.getPathForSeries("Mandalorian");

        assertFalse(Files.exists(seriesPath));
    }

    @Test
    public void getSeasonPath_season1_createsSeasonDirectory() {
        Path seasonPath = directoryService.getDirectoryToSave("Mandalorian", 1);

        assertTrue(Files.exists(seasonPath));
        assertTrue(Files.isDirectory(seasonPath));
        assertEquals("S01", seasonPath.getFileName().toString());
        String parentName = seasonPath.getParent().getFileName().toString();
        assertEquals("Mandalorian", parentName);
    }

    @Test
    public void getSeasonPath_season10_createsSeasonDirectory() {
        Path seasonPath = directoryService.getDirectoryToSave("Mandalorian", 10);

        assertTrue(Files.exists(seasonPath));
        assertTrue(Files.isDirectory(seasonPath));
        assertEquals("S10", seasonPath.getFileName().toString());
        String parentName = seasonPath.getParent().getFileName().toString();
        assertEquals("Mandalorian", parentName);
    }

    @Test
    public void getFileHierarchy_newSeries_episodeFound() throws IOException {
        Path seriesPath = rootFolder.getRoot().toPath().resolve("Series");
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