package ch.andreskonrad.torenta.library.service;

import ch.andreskonrad.torenta.bittorrent.service.BitTorrentService;
import ch.andreskonrad.torenta.directory.dto.DirectoryDto;
import ch.andreskonrad.torenta.directory.service.DirectoryService;
import ch.andreskonrad.torenta.library.dto.Series;
import ch.andreskonrad.torenta.library.dto.TvLibrary;
import ch.andreskonrad.torenta.tmdb.dto.TmdbEpisodeDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSearchResultDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSeriesDetailDto;
import ch.andreskonrad.torenta.tmdb.service.TmdbService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Set;

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

    public TvLibrary getTvLibrary() {
        Set<DirectoryDto> series = this.directoryService.getDirectoryHierarchy()
                .getSeriesRootDirectoryDto()
                .getSeries();

        TvLibrary tvLibrary = new TvLibrary();
        for (DirectoryDto seriesDirectoryDto : series) {
            Series seriesEntry = getSeriesEntry(seriesDirectoryDto);
            tvLibrary.addSeries(seriesEntry);
        }
        return tvLibrary;
    }

    private Series getSeriesEntry(DirectoryDto seriesDirectoryDto) {
        String seriesName = seriesDirectoryDto.getName();
        TmdbSearchResultDto tmdbSearchResultDto = tmdbService.search(seriesName);
        int seriesId = getId(tmdbSearchResultDto, seriesName);

        TmdbSeriesDetailDto seriesDetail = tmdbService.get(seriesId);


        HashMap<Integer, TmdbEpisodeDto[]> episodesBySeasonNumber = new HashMap<>();
        HashMap<Integer, DirectoryDto> seasonDirectoriesBySeasonNumber = new HashMap<>();
        for (DirectoryDto seasonDirectory : seriesDirectoryDto.getDirectories()) {
            int seasonNumber = getSeasonNumberForFolderName(seasonDirectory.getName());
            if (seasonNumber != -1) {
                episodesBySeasonNumber.put(seasonNumber, tmdbService.getEpisodes(seriesId, seasonNumber));
                seasonDirectoriesBySeasonNumber.put(seasonNumber, seasonDirectory);
            }
        }

        Series series = new Series(seriesDirectoryDto, seriesDetail, episodesBySeasonNumber, seasonDirectoriesBySeasonNumber, bitTorrentService.getAllDownloadDtos());
        return series;
    }

    private Integer getSeasonNumberForFolderName(String folderName) {
        try {
            return Integer.valueOf(folderName.replaceAll("[^\\d]", ""));
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

    }

    private int getId(TmdbSearchResultDto tmdbSearchResultDto, String seriesName) {
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
