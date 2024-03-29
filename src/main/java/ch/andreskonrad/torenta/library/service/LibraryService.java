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

import java.util.HashMap;

@Service
public class LibraryService {

    private final DirectoryService directoryService;
    private final TmdbService tmdbService;
    private final BitTorrentService bitTorrentService;

    @Autowired
    public LibraryService(TmdbService tmdbService, DirectoryService directoryService, BitTorrentService bitTorrentService) {
        this.directoryService = directoryService;
        this.tmdbService = tmdbService;
        this.bitTorrentService = bitTorrentService;
    }


    public Series getSeriesInLibrary(String seriesName) {
        DirectoryDto seriesDirectory = this.directoryService.getSeriesDirectory(seriesName);

        return getSeriesEntry(seriesDirectory);
    }

    private Series getSeriesEntry(DirectoryDto seriesDirectoryDto) {
        String seriesName = seriesDirectoryDto.getName();
        TmdbSeriesSearchResultDto tmdbSearchResultDto = tmdbService.searchSeries(seriesName);
        int seriesId = getId(tmdbSearchResultDto, seriesName);

        TmdbSeriesDetailDto seriesDetail = tmdbService.getSeries(seriesId);


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
        try {
            return Integer.valueOf(folderName.replaceAll("[^\\d]", ""));
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

    }

    private int getId(TmdbSeriesSearchResultDto tmdbSearchResultDto, String seriesName) {
        switch (tmdbSearchResultDto.getResults().size()) {
            case 0:
                return -1;
            case 1:
                return tmdbSearchResultDto.getResults().get(0).getId();
            default:
                return tmdbSearchResultDto.getResults().stream()
                        .filter(seriesOverview -> seriesOverview.getName().equals(seriesName))
                        .min((s1, s2) -> (int) s2.getPopularity() - (int) s1.getPopularity())
                        .get().getId();
        }
    }
}
