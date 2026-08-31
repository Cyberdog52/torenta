package ch.andreskonrad.torenta.library.dto;

import ch.andreskonrad.torenta.bittorrent.dto.DownloadDto;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadRequest;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadState;
import ch.andreskonrad.torenta.directory.dto.DirectoryDto;
import ch.andreskonrad.torenta.directory.dto.FileDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbEpisodeDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSeasonDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SeasonTest {

    @Test
    public void aggregateStatuses_allEpisodesComplete_returnsAiredAndDownloaded() {
        TmdbSeasonDto seasonDto = seasonDto(1);
        TmdbEpisodeDto first = episodeDto(1, 1, LocalDate.now().minusDays(2).toString());
        TmdbEpisodeDto second = episodeDto(1, 2, LocalDate.now().minusDays(1).toString());

        Season season = new Season(
                seasonDto,
                new TmdbEpisodeDto[]{first, second},
                directoryWithFiles("Show.S01E01.mkv", "Show.S01E02.mkv"),
                Set.of());

        assertEquals(AirStatus.AIRED, season.getAirStatus());
        assertEquals(DownloadStatus.DOWNLOADED, season.getDownloadStatus());
        assertEquals(2, season.getEpisodeList().size());
        assertSame(seasonDto, season.getTmdbSeasonDto());
        assertEquals(1, season.getSeasonNumber());
    }

    @Test
    public void aggregateStatuses_noEpisodesComplete_returnsNotAiredAndNotDownloaded() {
        TmdbEpisodeDto first = episodeDto(1, 1, LocalDate.now().plusDays(1).toString());
        TmdbEpisodeDto second = episodeDto(1, 2, LocalDate.now().plusDays(2).toString());

        Season season = new Season(
                seasonDto(1),
                new TmdbEpisodeDto[]{first, second},
                emptyDirectory(),
                Set.of());

        assertEquals(AirStatus.NOT_AIRED, season.getAirStatus());
        assertEquals(DownloadStatus.NOT_DOWNLOADED, season.getDownloadStatus());
    }

    @Test
    public void aggregateStatuses_someEpisodesComplete_returnsStartedAndDownloading() {
        TmdbEpisodeDto first = episodeDto(1, 1, LocalDate.now().minusDays(1).toString());
        TmdbEpisodeDto second = episodeDto(1, 2, LocalDate.now().plusDays(1).toString());

        Season season = new Season(
                seasonDto(1),
                new TmdbEpisodeDto[]{first, second},
                directoryWithFiles("Show.S01E01.mkv"),
                Set.of());

        assertEquals(AirStatus.STARTED, season.getAirStatus());
        assertEquals(DownloadStatus.DOWNLOADING, season.getDownloadStatus());
    }

    @Test
    public void episodeDownloads_nullRequestsAreIgnoredAndMatchingRequestIsApplied() {
        TmdbEpisodeDto episodeDto = episodeDto(1, 1, null);
        DownloadDto nullRequestDownload = mock(DownloadDto.class);
        DownloadDto nullEpisodeDownload = mock(DownloadDto.class);
        DownloadRequest nullEpisodeRequest = mock(DownloadRequest.class);
        DownloadDto matchingDownload = mock(DownloadDto.class);
        DownloadRequest matchingRequest = mock(DownloadRequest.class);

        when(nullRequestDownload.getDownloadRequest()).thenReturn(null);
        when(nullEpisodeDownload.getDownloadRequest()).thenReturn(nullEpisodeRequest);
        when(nullEpisodeRequest.getTmdbEpisode()).thenReturn(null);
        when(matchingDownload.getDownloadRequest()).thenReturn(matchingRequest);
        when(matchingRequest.getTmdbEpisode()).thenReturn(episodeDto);
        when(matchingDownload.getState()).thenReturn(DownloadState.STARTED);

        Season season = new Season(
                seasonDto(1),
                new TmdbEpisodeDto[]{episodeDto},
                emptyDirectory(),
                Set.of(nullRequestDownload, nullEpisodeDownload, matchingDownload));

        assertEquals(DownloadStatus.DOWNLOADING, season.getEpisodeList().getFirst().getDownloadStatus());
    }

    private static TmdbSeasonDto seasonDto(int seasonNumber) {
        TmdbSeasonDto seasonDto = mock(TmdbSeasonDto.class);
        when(seasonDto.getSeason_number()).thenReturn(seasonNumber);
        return seasonDto;
    }

    private static TmdbEpisodeDto episodeDto(int seasonNumber, int episodeNumber, String airDate) {
        TmdbEpisodeDto episodeDto = mock(TmdbEpisodeDto.class);
        when(episodeDto.getSeason_number()).thenReturn(seasonNumber);
        when(episodeDto.getEpisode_number()).thenReturn(episodeNumber);
        when(episodeDto.getAir_date()).thenReturn(airDate);
        return episodeDto;
    }

    private static DirectoryDto emptyDirectory() {
        return new DirectoryDto("season", Set.of(), Set.of(), "/shows");
    }

    private static DirectoryDto directoryWithFiles(String... names) {
        Set<FileDto> files = java.util.Arrays.stream(names)
                .map(name -> new FileDto("/shows/" + name, name))
                .collect(java.util.stream.Collectors.toSet());
        return new DirectoryDto("season", files, Set.of(), "/shows");
    }
}
