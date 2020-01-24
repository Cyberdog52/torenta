package ch.andreskonrad.torenta.library.service;

import ch.andreskonrad.torenta.bittorrent.service.BitTorrentService;
import ch.andreskonrad.torenta.directory.dto.DirectoryDto;
import ch.andreskonrad.torenta.directory.service.DirectoryService;
import ch.andreskonrad.torenta.library.dto.SeriesEntry;
import ch.andreskonrad.torenta.library.dto.TvLibrary;
import ch.andreskonrad.torenta.tmdb.dto.Episode;
import ch.andreskonrad.torenta.tmdb.dto.SearchResult;
import ch.andreskonrad.torenta.tmdb.dto.SeriesDetail;
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
            SeriesEntry seriesEntry = getSeriesEntry(seriesDirectoryDto);
            tvLibrary.addSeries(seriesEntry);
        }
        return tvLibrary;
    }

    private SeriesEntry getSeriesEntry(DirectoryDto seriesDirectoryDto) {
        String seriesName = seriesDirectoryDto.getName();
        SearchResult searchResult = tmdbService.search(seriesName);
        int seriesId = getId(searchResult, seriesName);

        SeriesDetail seriesDetail = tmdbService.get(seriesId);


        HashMap<Integer, Episode[]> episodesBySeasonNumber = new HashMap<>();
        HashMap<Integer, DirectoryDto> seasonDirectoriesBySeasonNumber = new HashMap<>();
        for (DirectoryDto seasonDirectory : seriesDirectoryDto.getDirectories()) {
            int seasonNumber = getSeasonNumberForFolderName(seasonDirectory.getName());
            if (seasonNumber != -1) {
                episodesBySeasonNumber.put(seasonNumber, tmdbService.getEpisodes(seriesId, seasonNumber));
                seasonDirectoriesBySeasonNumber.put(seasonNumber, seasonDirectory);
            }
        }

        SeriesEntry seriesEntry = new SeriesEntry(seriesDirectoryDto, seriesDetail, episodesBySeasonNumber, seasonDirectoriesBySeasonNumber, bitTorrentService.getAllDownloadDtos());
        return seriesEntry;
    }

    private Integer getSeasonNumberForFolderName(String folderName) {
        try {
            return Integer.valueOf(folderName.replaceAll("[^\\d]", ""));
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

    }

    private int getId(SearchResult searchResult, String seriesName) {
        switch (searchResult.getResults().size()) {
            case 0:
                return -1;
            case 1:
                return searchResult.getResults().get(0).getId();
            default:
                return searchResult.getResults().stream()
                        .filter(seriesOverview -> seriesOverview.getName().equals(seriesName))
                        .min((s1, s2) -> (int) s2.getPopularity() - (int) s1.getPopularity())
                        .get().getId();
        }
    }
}
