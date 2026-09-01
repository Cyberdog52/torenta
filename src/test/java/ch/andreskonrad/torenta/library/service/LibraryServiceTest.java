package ch.andreskonrad.torenta.library.service;

import ch.andreskonrad.torenta.bittorrent.dto.DownloadDto;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadRequest;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadState;
import ch.andreskonrad.torenta.bittorrent.service.BitTorrentService;
import ch.andreskonrad.torenta.directory.dto.DirectoryDto;
import ch.andreskonrad.torenta.directory.service.DirectoryService;
import ch.andreskonrad.torenta.library.dto.DownloadStatus;
import ch.andreskonrad.torenta.library.dto.Series;
import ch.andreskonrad.torenta.tmdb.dto.TmdbEpisodeDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSeriesDetailDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSeriesOverviewDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSeriesSearchResultDto;
import ch.andreskonrad.torenta.tmdb.service.TmdbService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LibraryServiceTest {

    @Mock
    private TmdbService tmdbService;
    @Mock
    private DirectoryService directoryService;
    @Mock
    private BitTorrentService bitTorrentService;

    private LibraryService libraryService;

    @BeforeEach
    public void setUp() {
        libraryService = new LibraryService(tmdbService, directoryService, bitTorrentService);
    }

    @Test
    public void getSeriesInLibrary_noTmdbResults_throwsControlledException() {
        DirectoryDto seriesDirectory = directory("Unknown");
        TmdbSeriesSearchResultDto searchResult = searchResult(List.of());
        when(directoryService.getSeriesDirectory("Unknown")).thenReturn(seriesDirectory);
        when(tmdbService.searchSeries("Unknown")).thenReturn(searchResult);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> libraryService.getSeriesInLibrary("Unknown"));

        assertEquals("No TMDB series found for: Unknown", exception.getMessage());
        verify(tmdbService, never()).getSeries(anyInt());
    }

    @Test
    public void getSeriesInLibrary_oneTmdbResult_usesItsId() {
        DirectoryDto seriesDirectory = directory("The Show");
        TmdbSeriesOverviewDto overview = overview(17, "Different API Name", 1.0);
        TmdbSeriesDetailDto detail = detail();
        stubSuccessfulLookup(seriesDirectory, List.of(overview), detail, Set.of());

        Series series = libraryService.getSeriesInLibrary("The Show");

        assertSame(seriesDirectory, series.getDirectoryDto());
        assertSame(detail, series.getSeriesDetail());
        verify(tmdbService).getSeries(17);
    }

    @Test
    public void getSeriesInLibrary_multipleTmdbResults_selectsExactName() {
        DirectoryDto seriesDirectory = directory("The Show");
        TmdbSeriesOverviewDto popularNonMatch = overview(99, "The Show UK", 100.0);
        TmdbSeriesOverviewDto exactMatch = overview(23, "The Show", 2.0);
        TmdbSeriesDetailDto detail = detail();
        stubSuccessfulLookup(
                seriesDirectory,
                List.of(popularNonMatch, exactMatch),
                detail,
                Set.of());

        libraryService.getSeriesInLibrary("The Show");

        verify(tmdbService).getSeries(23);
        verify(tmdbService, never()).getSeries(99);
    }

    @Test
    public void getSeriesInLibrary_multipleExactMatches_selectsMostPopular() {
        DirectoryDto seriesDirectory = directory("The Show");
        TmdbSeriesOverviewDto lessPopular = overview(5, "The Show", 10.1);
        TmdbSeriesOverviewDto morePopular = overview(6, "The Show", 10.9);
        stubSuccessfulLookup(
                seriesDirectory,
                List.of(lessPopular, morePopular),
                detail(),
                Set.of());

        libraryService.getSeriesInLibrary("The Show");

        verify(tmdbService).getSeries(6);
    }

    @Test
    public void getSeriesInLibrary_multipleTmdbResultsWithoutExactMatch_throwsControlledException() {
        DirectoryDto seriesDirectory = directory("The Show");
        TmdbSeriesSearchResultDto searchResult = searchResult(List.of(
                overview(1, "The Show UK", 10.0),
                overview(2, "The Show US", 20.0)));
        when(directoryService.getSeriesDirectory("The Show")).thenReturn(seriesDirectory);
        when(tmdbService.searchSeries("The Show")).thenReturn(searchResult);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> libraryService.getSeriesInLibrary("The Show"));

        assertEquals("No exact TMDB series match found for: The Show", exception.getMessage());
        verify(tmdbService, never()).getSeries(anyInt());
    }

    @Test
    public void getSeriesInLibrary_validSeasonFolders_loadEpisodesAndForwardsDownloads() {
        DirectoryDto seasonOne = directory("S01");
        DirectoryDto seasonTwelve = directory("Season 12");
        DirectoryDto seriesDirectory = directory("The Show", seasonOne, seasonTwelve);
        TmdbEpisodeDto seasonOneEpisode = episode(1, 1);
        TmdbEpisodeDto[] seasonOneEpisodes = {seasonOneEpisode};
        TmdbEpisodeDto[] seasonTwelveEpisodes = {};
        DownloadRequest request = new DownloadRequest(seasonOneEpisode, null, null, null);
        DownloadDto download = new DownloadDto(
                "1", DownloadState.STARTED, null, null, 0.5, request, 0L, 0L, 0L, 0.0, null,
                new ch.andreskonrad.torenta.bittorrent.dto.DownloadActionCapabilities(true, false, true, false));
        TmdbSeriesDetailDto detail = detail();
        stubSuccessfulLookup(seriesDirectory, List.of(overview(42, "The Show", 1.0)), detail, Set.of(download));
        when(tmdbService.getEpisodes(42, 1)).thenReturn(seasonOneEpisodes);
        when(tmdbService.getEpisodes(42, 12)).thenReturn(seasonTwelveEpisodes);

        Series series = libraryService.getSeriesInLibrary("The Show");

        assertSame(seasonOneEpisodes, series.getEpisodesBySeasonNumber().get(1));
        assertSame(seasonTwelveEpisodes, series.getEpisodesBySeasonNumber().get(12));
        assertSame(seasonOne, series.getSeasonDirectoriesBySeasonNumber().get(1));
        assertSame(seasonTwelve, series.getSeasonDirectoriesBySeasonNumber().get(12));
        verify(tmdbService).getEpisodes(42, 1);
        verify(tmdbService).getEpisodes(42, 12);
        verify(bitTorrentService).getAllDownloadDtos();
        assertTrue(series.getSeasonList().stream()
                .flatMap(season -> season.getEpisodeList().stream())
                .anyMatch(episode -> episode.getDownloadStatus() == DownloadStatus.DOWNLOADING));
    }

    @Test
    public void getSeriesInLibrary_malformedSeasonFolders_areIgnored() {
        DirectoryDto seriesDirectory = directory(
                "The Show",
                directory("Specials"),
                directory("S01E02"),
                directory("Season"));
        stubSuccessfulLookup(
                seriesDirectory,
                List.of(overview(42, "The Show", 1.0)),
                detail(),
                Set.of());

        Series series = libraryService.getSeriesInLibrary("The Show");

        assertTrue(series.getEpisodesBySeasonNumber().isEmpty());
        verify(tmdbService, never()).getEpisodes(anyInt(), anyInt());
    }

    @Test
    public void getSeriesInLibrary_missingSeriesDirectory_throwsControlledException() {
        when(directoryService.getSeriesDirectory("Missing")).thenReturn(null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> libraryService.getSeriesInLibrary("Missing"));

        assertEquals("Series directory does not exist: Missing", exception.getMessage());
        verify(tmdbService, never()).searchSeries("Missing");
    }

    private void stubSuccessfulLookup(
            DirectoryDto seriesDirectory,
            List<TmdbSeriesOverviewDto> overviews,
            TmdbSeriesDetailDto detail,
            Set<DownloadDto> downloads
    ) {
        TmdbSeriesSearchResultDto searchResult = searchResult(overviews);
        when(directoryService.getSeriesDirectory(seriesDirectory.getName())).thenReturn(seriesDirectory);
        when(tmdbService.searchSeries(seriesDirectory.getName())).thenReturn(searchResult);
        when(tmdbService.getSeries(anyInt())).thenReturn(detail);
        when(bitTorrentService.getAllDownloadDtos()).thenReturn(downloads);
    }

    private static DirectoryDto directory(String name, DirectoryDto... directories) {
        return new DirectoryDto(name, Set.of(), Set.of(directories), "/library/" + name);
    }

    private static TmdbSeriesSearchResultDto searchResult(List<TmdbSeriesOverviewDto> results) {
        TmdbSeriesSearchResultDto searchResult = org.mockito.Mockito.mock(TmdbSeriesSearchResultDto.class);
        when(searchResult.getResults()).thenReturn(results);
        return searchResult;
    }

    private static TmdbSeriesOverviewDto overview(int id, String name, double popularity) {
        TmdbSeriesOverviewDto overview = org.mockito.Mockito.mock(TmdbSeriesOverviewDto.class);
        lenient().when(overview.getId()).thenReturn(id);
        lenient().when(overview.getName()).thenReturn(name);
        lenient().when(overview.getPopularity()).thenReturn(popularity);
        return overview;
    }

    private static TmdbSeriesDetailDto detail() {
        TmdbSeriesDetailDto detail = org.mockito.Mockito.mock(TmdbSeriesDetailDto.class);
        lenient().when(detail.getSeasons()).thenReturn(List.of());
        return detail;
    }

    private static TmdbEpisodeDto episode(int seasonNumber, int episodeNumber) {
        TmdbEpisodeDto episode = org.mockito.Mockito.mock(TmdbEpisodeDto.class);
        when(episode.getSeason_number()).thenReturn(seasonNumber);
        when(episode.getEpisode_number()).thenReturn(episodeNumber);
        when(episode.getAir_date()).thenReturn(null);
        return episode;
    }
}
