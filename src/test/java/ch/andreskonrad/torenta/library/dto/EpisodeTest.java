package ch.andreskonrad.torenta.library.dto;

import ch.andreskonrad.torenta.bittorrent.dto.DownloadDto;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadState;
import ch.andreskonrad.torenta.directory.dto.DirectoryDto;
import ch.andreskonrad.torenta.directory.dto.FileDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbEpisodeDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EpisodeTest {

    @Test
    public void episodeString_singleAndDoubleDigitNumbers_formatsWithRequiredPadding() {
        TmdbEpisodeDto singleDigitDto = episodeDto(1, 2, null);
        TmdbEpisodeDto doubleDigitDto = episodeDto(10, 11, null);

        Episode singleDigitEpisode = new Episode(singleDigitDto, emptyDirectory(), null);
        Episode doubleDigitEpisode = new Episode(doubleDigitDto, emptyDirectory(), null);

        assertEquals("S01E02", singleDigitEpisode.getEpisodeString());
        assertEquals(1, singleDigitEpisode.getSeasonNumber());
        assertEquals(2, singleDigitEpisode.getEpisodeNumber());
        assertSame(singleDigitDto, singleDigitEpisode.getTmdbEpisodeDto());
        assertEquals("S10E11", doubleDigitEpisode.getEpisodeString());
    }

    @Test
    public void airStatus_missingFutureAndPastDates_returnsExpectedStatus() {
        Episode missingDate = new Episode(episodeDto(1, 1, null), emptyDirectory(), null);
        Episode future = new Episode(
                episodeDto(1, 2, LocalDate.now().plusDays(1).toString()),
                emptyDirectory(),
                null);
        Episode past = new Episode(
                episodeDto(1, 3, LocalDate.now().minusDays(1).toString()),
                emptyDirectory(),
                null);

        assertEquals(AirStatus.NOT_AIRED, missingDate.getAirStatus());
        assertEquals(AirStatus.NOT_AIRED, future.getAirStatus());
        assertEquals(AirStatus.AIRED, past.getAirStatus());
    }

    @Test
    public void downloadStatus_episodeInNestedDirectory_isDownloadedCaseInsensitively() {
        DirectoryDto nestedDirectory = new DirectoryDto(
                "nested",
                Set.of(new FileDto("/shows/episode.mkv", "Show.S01E02.episode.mkv")),
                Set.of(),
                "/shows");
        DirectoryDto seasonDirectory = new DirectoryDto(
                "season",
                Set.of(),
                Set.of(new DirectoryDto("child", Set.of(), Set.of(nestedDirectory), "/shows/child")),
                "/shows");

        Episode episode = new Episode(episodeDto(1, 2, null), seasonDirectory, null);

        assertEquals(DownloadStatus.DOWNLOADED, episode.getDownloadStatus());
    }

    @Test
    public void downloadStatus_downloadStateTakesPrecedenceOverDirectoryContents() {
        DirectoryDto directoryWithEpisode = directoryWithFiles("Show.S01E01.mkv");

        Episode started = new Episode(
                episodeDto(1, 1, null),
                directoryWithEpisode,
                downloadDto(DownloadState.STARTED));
        Episode cancelled = new Episode(
                episodeDto(1, 1, null),
                directoryWithEpisode,
                downloadDto(DownloadState.CANCELLED));
        Episode finished = new Episode(
                episodeDto(1, 1, null),
                emptyDirectory(),
                downloadDto(DownloadState.FINISHED));

        assertEquals(DownloadStatus.DOWNLOADING, started.getDownloadStatus());
        assertEquals(DownloadStatus.NOT_DOWNLOADED, cancelled.getDownloadStatus());
        assertEquals(DownloadStatus.DOWNLOADED, finished.getDownloadStatus());
    }

    @Test
    public void downloadStatus_downloadWithNullState_fallsBackToDirectoryContents() {
        DownloadDto downloadDto = mock(DownloadDto.class);
        when(downloadDto.getState()).thenReturn(null);

        Episode episode = new Episode(
                episodeDto(1, 1, null),
                directoryWithFiles("Show.S01E01.mkv"),
                downloadDto);

        assertEquals(DownloadStatus.DOWNLOADED, episode.getDownloadStatus());
    }

    private static TmdbEpisodeDto episodeDto(int seasonNumber, int episodeNumber, String airDate) {
        TmdbEpisodeDto episodeDto = mock(TmdbEpisodeDto.class);
        when(episodeDto.getSeason_number()).thenReturn(seasonNumber);
        when(episodeDto.getEpisode_number()).thenReturn(episodeNumber);
        when(episodeDto.getAir_date()).thenReturn(airDate);
        return episodeDto;
    }

    private static DownloadDto downloadDto(DownloadState state) {
        DownloadDto downloadDto = mock(DownloadDto.class);
        when(downloadDto.getState()).thenReturn(state);
        return downloadDto;
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
