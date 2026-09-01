package ch.andreskonrad.torenta.recommendation.service;

import ch.andreskonrad.torenta.directory.dto.DirectoryDto;
import ch.andreskonrad.torenta.directory.dto.FileDto;
import ch.andreskonrad.torenta.directory.service.DirectoryService;
import ch.andreskonrad.torenta.library.dto.Series;
import ch.andreskonrad.torenta.library.service.LibraryService;
import ch.andreskonrad.torenta.recommendation.dto.SeriesRecommendationDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbEpisodeDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSeasonDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSeriesDetailDto;
import ch.andreskonrad.torenta.tmdb.service.TmdbService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RecommendationServiceTest {

    @Mock
    private DirectoryService directoryService;
    @Mock
    private LibraryService libraryService;
    @Mock
    private TmdbService tmdbService;

    private RecommendationService recommendationService;

    @BeforeEach
    public void setUp() {
        recommendationService = new RecommendationService(directoryService, libraryService, tmdbService);
        DOWNLOADED_EPISODE_IDS.clear();
    }

    @Test
    public void getRecommendations_passesWeeksAsDurationToDirectoryService() {
        when(directoryService.getSeriesNamesModifiedWithin(any())).thenReturn(List.of());

        recommendationService.getRecommendations(3);

        verify(directoryService).getSeriesNamesModifiedWithin(Duration.ofDays(21));
    }

    @Test
    public void getRecommendations_zeroWeeks_scansEntireLibraryIgnoringRecency() {
        when(directoryService.getAllSeriesNames()).thenReturn(List.of());

        recommendationService.getRecommendations(0);

        verify(directoryService).getAllSeriesNames();
        verify(directoryService, never()).getSeriesNamesModifiedWithin(any());
    }

    @Test
    public void getRecommendations_negativeWeeks_scansEntireLibraryIgnoringRecency() {
        when(directoryService.getAllSeriesNames()).thenReturn(List.of());

        recommendationService.getRecommendations(-1);

        verify(directoryService).getAllSeriesNames();
        verify(directoryService, never()).getSeriesNamesModifiedWithin(any());
    }

    @Test
    public void getRecommendations_seriesFullyCaughtUp_isExcluded() {
        when(directoryService.getSeriesNamesModifiedWithin(any())).thenReturn(List.of("Caught Up Show"));
        Series series = series(detail(1, "Caught Up Show"), season(1, episode(1, 1, past(), true)));
        when(libraryService.getSeriesInLibrary("Caught Up Show")).thenReturn(series);

        List<SeriesRecommendationDto> recommendations = recommendationService.getRecommendations(2);

        assertTrue(recommendations.isEmpty());
    }

    @Test
    public void getRecommendations_seriesWithMissingEpisodesAcrossSeasons_recommendsUpToThreeChronologically() {
        when(directoryService.getSeriesNamesModifiedWithin(any())).thenReturn(List.of("The Office"));
        Series series = series(
                detail(1, "The Office"),
                season(1, episode(1, 1, past(), true), episode(1, 2, past(), true)),
                season(2, episode(2, 1, past(), true), episode(2, 2, past(), false)),
                season(3,
                        episode(3, 1, past(), true),
                        episode(3, 2, past(), false),
                        episode(3, 3, past(), false),
                        episode(3, 4, past(), false)));
        when(libraryService.getSeriesInLibrary("The Office")).thenReturn(series);

        List<SeriesRecommendationDto> recommendations = recommendationService.getRecommendations(2);

        assertEquals(1, recommendations.size());
        SeriesRecommendationDto recommendation = recommendations.get(0);
        assertEquals("The Office", recommendation.getSeriesName());
        assertEquals(3, recommendation.getRecommendedEpisodes().size());
        assertEquals("S02E02", recommendation.getRecommendedEpisodes().get(0).getEpisodeString());
        assertEquals("S03E02", recommendation.getRecommendedEpisodes().get(1).getEpisodeString());
        assertEquals("S03E03", recommendation.getRecommendedEpisodes().get(2).getEpisodeString());
    }

    @Test
    public void getRecommendations_unresolvableSeries_isSkippedWithoutThrowing() {
        when(directoryService.getSeriesNamesModifiedWithin(any())).thenReturn(List.of("Unknown"));
        when(libraryService.getSeriesInLibrary("Unknown")).thenThrow(new IllegalStateException("boom"));

        List<SeriesRecommendationDto> recommendations = recommendationService.getRecommendations(2);

        assertTrue(recommendations.isEmpty());
    }

    @Test
    public void getRecommendations_lastLocalSeasonFullyDownloaded_recommendsNextUnstartedSeason() {
        when(directoryService.getSeriesNamesModifiedWithin(any())).thenReturn(List.of("The Office"));
        TmdbSeriesDetailDto detail = detail(1, "The Office");
        TmdbSeasonDto nextSeason = mock(TmdbSeasonDto.class);
        lenient().when(nextSeason.getSeason_number()).thenReturn(2);
        when(detail.getSeasons()).thenReturn(List.of(nextSeason));
        Series series = series(detail, season(1, episode(1, 1, past(), true)));
        when(libraryService.getSeriesInLibrary("The Office")).thenReturn(series);
        TmdbEpisodeDto nextSeasonEpisode = episode(2, 1, past(), false);
        when(tmdbService.getEpisodes(1, 2)).thenReturn(new TmdbEpisodeDto[]{ nextSeasonEpisode });

        List<SeriesRecommendationDto> recommendations = recommendationService.getRecommendations(2);

        assertEquals(1, recommendations.size());
        assertEquals("S02E01", recommendations.get(0).getRecommendedEpisodes().get(0).getEpisodeString());
    }

    @Test
    public void getRecommendations_nextSeasonNotYetAired_isNotRecommended() {
        when(directoryService.getSeriesNamesModifiedWithin(any())).thenReturn(List.of("The Office"));
        TmdbSeriesDetailDto detail = detail(1, "The Office");
        TmdbSeasonDto nextSeason = mock(TmdbSeasonDto.class);
        lenient().when(nextSeason.getSeason_number()).thenReturn(2);
        when(detail.getSeasons()).thenReturn(List.of(nextSeason));
        Series series = series(detail, season(1, episode(1, 1, past(), true)));
        when(libraryService.getSeriesInLibrary("The Office")).thenReturn(series);
        TmdbEpisodeDto notYetAiredEpisode = episode(2, 1, future(), false);
        when(tmdbService.getEpisodes(eq(1), eq(2))).thenReturn(new TmdbEpisodeDto[]{ notYetAiredEpisode });

        List<SeriesRecommendationDto> recommendations = recommendationService.getRecommendations(2);

        assertTrue(recommendations.isEmpty());
        verify(tmdbService, never()).getEpisodes(eq(1), eq(3));
    }

    private static Series series(TmdbSeriesDetailDto detail, SeasonFixture... seasons) {
        HashMap<Integer, TmdbEpisodeDto[]> episodesBySeasonNumber = new HashMap<>();
        HashMap<Integer, DirectoryDto> seasonDirectoriesBySeasonNumber = new HashMap<>();
        for (SeasonFixture season : seasons) {
            episodesBySeasonNumber.put(season.seasonNumber, season.episodes);
            seasonDirectoriesBySeasonNumber.put(season.seasonNumber, directory("S" + season.seasonNumber, season.downloadedFiles));
        }
        return new Series(directory("series", Set.of()), detail, episodesBySeasonNumber, seasonDirectoriesBySeasonNumber, Set.of());
    }

    private static SeasonFixture season(int seasonNumber, TmdbEpisodeDto... episodes) {
        SeasonFixture fixture = new SeasonFixture();
        fixture.seasonNumber = seasonNumber;
        fixture.episodes = episodes;
        fixture.downloadedFiles = Set.of(episodes).stream()
                .filter(e -> DOWNLOADED_EPISODE_IDS.contains(System.identityHashCode(e)))
                .map(e -> new FileDto("/library/S" + seasonNumber + "/" + episodeString(e) + ".mkv", episodeString(e) + ".mkv"))
                .collect(Collectors.toSet());
        return fixture;
    }

    private static class SeasonFixture {
        int seasonNumber;
        TmdbEpisodeDto[] episodes;
        Set<FileDto> downloadedFiles;
    }

    private static String episodeString(TmdbEpisodeDto episode) {
        StringBuilder s = new StringBuilder().append("S");
        if (episode.getSeason_number() < 10) s.append(0);
        s.append(episode.getSeason_number());
        s.append("E");
        if (episode.getEpisode_number() < 10) s.append(0);
        s.append(episode.getEpisode_number());
        return s.toString();
    }

    private static final Set<Integer> DOWNLOADED_EPISODE_IDS = new HashSet<>();

    private static DirectoryDto directory(String name, Set<FileDto> files) {
        return new DirectoryDto(name, files, Set.of(), "/library/" + name);
    }

    private static TmdbSeriesDetailDto detail(int id, String name) {
        TmdbSeriesDetailDto detail = mock(TmdbSeriesDetailDto.class);
        lenient().when(detail.getId()).thenReturn(id);
        lenient().when(detail.getName()).thenReturn(name);
        lenient().when(detail.getSeasons()).thenReturn(List.of());
        lenient().when(detail.getPoster_path()).thenReturn("/poster.jpg");
        return detail;
    }

    private static TmdbEpisodeDto episode(int seasonNumber, int episodeNumber, String airDate, boolean downloaded) {
        TmdbEpisodeDto episode = mock(TmdbEpisodeDto.class);
        lenient().when(episode.getSeason_number()).thenReturn(seasonNumber);
        lenient().when(episode.getEpisode_number()).thenReturn(episodeNumber);
        lenient().when(episode.getAir_date()).thenReturn(airDate);
        lenient().when(episode.getName()).thenReturn("Episode " + episodeNumber);
        if (downloaded) {
            DOWNLOADED_EPISODE_IDS.add(System.identityHashCode(episode));
        }
        return episode;
    }

    private static String past() {
        return LocalDate.now().minusDays(7).toString();
    }

    private static String future() {
        return LocalDate.now().plusDays(7).toString();
    }
}
