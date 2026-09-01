package ch.andreskonrad.torenta.library.service;

import ch.andreskonrad.torenta.bittorrent.service.BitTorrentService;
import ch.andreskonrad.torenta.directory.dto.DirectoryDto;
import ch.andreskonrad.torenta.directory.service.DirectoryService;
import ch.andreskonrad.torenta.library.dto.Series;
import ch.andreskonrad.torenta.tmdb.dto.TmdbEpisodeDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSeriesSearchResultDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSeriesDetailDto;
import ch.andreskonrad.torenta.tmdb.service.TmdbService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LibraryService {

    private static final Pattern SEASON_DIRECTORY_PATTERN =
            Pattern.compile("^(?:S0*(\\d+)|Season\\s+0*(\\d+))$", Pattern.CASE_INSENSITIVE);

    private final DirectoryService directoryService;
    private final TmdbService tmdbService;
    private final BitTorrentService bitTorrentService;

    @Autowired
    public LibraryService(TmdbService tmdbService, DirectoryService directoryService, BitTorrentService bitTorrentService) {
        this.directoryService = directoryService;
        this.tmdbService = tmdbService;
        this.bitTorrentService = bitTorrentService;
    }


    /**
     * @return the series' library entry, or {@code null} if it hasn't been downloaded (i.e. no
     * matching directory exists). This is an everyday, expected condition (not every series a
     * user is browsing is already in their library), so it's signalled by a {@code null} return
     * rather than an exception.
     */
    public Series getSeriesInLibrary(String seriesName) {
        DirectoryDto seriesDirectory = this.directoryService.getSeriesDirectory(seriesName);
        if (seriesDirectory == null) {
            return null;
        }

        return getSeriesEntry(seriesDirectory);
    }

    private Series getSeriesEntry(DirectoryDto seriesDirectoryDto) {
        String seriesName = seriesDirectoryDto.getName();
        TmdbSeriesSearchResultDto tmdbSearchResultDto = tmdbService.searchSeries(seriesName);
        int seriesId = getId(tmdbSearchResultDto, seriesName);

        TmdbSeriesDetailDto seriesDetail = tmdbService.getSeries(seriesId);
        if (seriesDetail == null) {
            throw new IllegalStateException("TMDB series details are unavailable for: " + seriesName);
        }

        HashMap<Integer, TmdbEpisodeDto[]> episodesBySeasonNumber = new HashMap<>();
        HashMap<Integer, DirectoryDto> seasonDirectoriesBySeasonNumber = new HashMap<>();
        for (DirectoryDto seasonDirectory : seriesDirectoryDto.getDirectories()) {
            int seasonNumber = getSeasonNumberForFolderName(seasonDirectory.getName());
            if (seasonNumber != -1) {
                episodesBySeasonNumber.put(seasonNumber, tmdbService.getEpisodes(seriesId, seasonNumber));
                seasonDirectoriesBySeasonNumber.put(seasonNumber, seasonDirectory);
            }
        }

        return new Series(seriesDirectoryDto, seriesDetail, episodesBySeasonNumber, seasonDirectoriesBySeasonNumber, bitTorrentService.getAllDownloadDtos());
    }

    private Integer getSeasonNumberForFolderName(String folderName) {
        if (folderName == null) {
            return -1;
        }

        Matcher matcher = SEASON_DIRECTORY_PATTERN.matcher(folderName);
        if (!matcher.matches()) {
            return -1;
        }

        String seasonNumber = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        try {
            return Integer.valueOf(seasonNumber);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int getId(TmdbSeriesSearchResultDto tmdbSearchResultDto, String seriesName) {
        if (tmdbSearchResultDto == null || tmdbSearchResultDto.getResults() == null
                || tmdbSearchResultDto.getResults().isEmpty()) {
            throw new IllegalStateException("No TMDB series found for: " + seriesName);
        }

        switch (tmdbSearchResultDto.getResults().size()) {
            case 1:
                return tmdbSearchResultDto.getResults().get(0).getId();
            default:
                return tmdbSearchResultDto.getResults().stream()
                        .filter(seriesOverview -> seriesOverview.getName().equals(seriesName))
                        .max(Comparator.comparingDouble(seriesOverview -> seriesOverview.getPopularity()))
                        .orElseThrow(() -> new IllegalStateException(
                                "No exact TMDB series match found for: " + seriesName))
                        .getId();
        }
    }
}
