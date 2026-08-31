package ch.andreskonrad.torenta.library.dto;

import ch.andreskonrad.torenta.bittorrent.dto.DownloadDto;
import ch.andreskonrad.torenta.directory.dto.DirectoryDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbEpisodeDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSeasonDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSeriesDetailDto;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SeriesTest {

    @Test
    public void constructor_missingSeasonMetadata_stillBuildsSeasonAndPreservesInputs() {
        DirectoryDto seriesDirectory = mock(DirectoryDto.class);
        DirectoryDto firstSeasonDirectory = emptyDirectory("season-one");
        DirectoryDto missingMetadataDirectory = emptyDirectory("season-two");
        TmdbSeriesDetailDto seriesDetail = mock(TmdbSeriesDetailDto.class);
        TmdbSeasonDto firstSeasonMetadata = mock(TmdbSeasonDto.class);
        TmdbEpisodeDto firstEpisode = episodeDto(1, 1);
        TmdbEpisodeDto missingMetadataEpisode = episodeDto(2, 1);
        Map<Integer, TmdbEpisodeDto[]> episodesBySeasonNumber = new LinkedHashMap<>();
        HashMap<Integer, DirectoryDto> directoriesBySeasonNumber = new HashMap<>();

        when(firstSeasonMetadata.getSeason_number()).thenReturn(1);
        when(seriesDetail.getSeasons()).thenReturn(List.of(firstSeasonMetadata));
        episodesBySeasonNumber.put(1, new TmdbEpisodeDto[]{firstEpisode});
        episodesBySeasonNumber.put(2, new TmdbEpisodeDto[]{missingMetadataEpisode});
        directoriesBySeasonNumber.put(1, firstSeasonDirectory);
        directoriesBySeasonNumber.put(2, missingMetadataDirectory);

        Series series = new Series(
                seriesDirectory,
                seriesDetail,
                episodesBySeasonNumber,
                directoriesBySeasonNumber,
                Set.<DownloadDto>of());

        assertSame(seriesDirectory, series.getDirectoryDto());
        assertSame(seriesDetail, series.getSeriesDetail());
        assertSame(episodesBySeasonNumber, series.getEpisodesBySeasonNumber());
        assertSame(directoriesBySeasonNumber, series.getSeasonDirectoriesBySeasonNumber());
        assertEquals(2, series.getSeasonList().size());

        Season missingMetadataSeason = series.getSeasonList().stream()
                .filter(season -> season.getTmdbSeasonDto() == null)
                .findFirst()
                .orElseThrow();
        assertNull(missingMetadataSeason.getTmdbSeasonDto());
        assertEquals(-1, missingMetadataSeason.getSeasonNumber());
        assertEquals(1, missingMetadataSeason.getEpisodeList().size());
        assertSame(missingMetadataEpisode, missingMetadataSeason.getEpisodeList().getFirst().getTmdbEpisodeDto());
        assertTrue(series.getSeasonList().stream()
                .anyMatch(season -> season.getTmdbSeasonDto() == firstSeasonMetadata));
    }

    private static TmdbEpisodeDto episodeDto(int seasonNumber, int episodeNumber) {
        TmdbEpisodeDto episodeDto = mock(TmdbEpisodeDto.class);
        when(episodeDto.getSeason_number()).thenReturn(seasonNumber);
        when(episodeDto.getEpisode_number()).thenReturn(episodeNumber);
        when(episodeDto.getAir_date()).thenReturn(null);
        return episodeDto;
    }

    private static DirectoryDto emptyDirectory(String name) {
        return new DirectoryDto(name, Set.of(), Set.of(), "/shows/" + name);
    }
}
