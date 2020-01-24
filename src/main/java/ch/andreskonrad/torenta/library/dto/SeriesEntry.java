package ch.andreskonrad.torenta.library.dto;

import ch.andreskonrad.torenta.bittorrent.dto.DownloadDto;
import ch.andreskonrad.torenta.directory.dto.DirectoryDto;
import ch.andreskonrad.torenta.tmdb.dto.Episode;
import ch.andreskonrad.torenta.tmdb.dto.Season;
import ch.andreskonrad.torenta.tmdb.dto.SeriesDetail;

import java.util.*;

public class SeriesEntry {
    private final DirectoryDto directoryDto;

    private final SeriesDetail seriesDetail;
    private final Map<Integer, Episode[]> episodesBySeasonNumber;
    private final Map<Integer, DirectoryDto> seasonDirectoriesBySeasonNumber;
    private final List<SeasonEntry> seasonEntryList = new ArrayList<>();

    public SeriesEntry(DirectoryDto seriesDirectoryDto, SeriesDetail seriesDetail, Map<Integer, Episode[]> episodesBySeasonNumber, HashMap<Integer, DirectoryDto> seasonDirectoriesBySeasonNumber, Set<DownloadDto> downloadDtoSet) {
        this.directoryDto = seriesDirectoryDto;
        this.seriesDetail = seriesDetail;
        this.episodesBySeasonNumber = episodesBySeasonNumber;
        this.seasonDirectoriesBySeasonNumber = seasonDirectoriesBySeasonNumber;


        for (Integer seasonNumber : episodesBySeasonNumber.keySet()) {
            DirectoryDto seasonDirectory = seasonDirectoriesBySeasonNumber.get(seasonNumber);
            Episode[] episodes = episodesBySeasonNumber.get(seasonNumber);
            Season season = seriesDetail.getSeasons().stream()
                    .filter(s -> s.getSeason_number() == seasonNumber).findFirst()
                    .orElse(null);
            seasonEntryList.add(new SeasonEntry(season, episodes, seasonDirectory, downloadDtoSet));
        }
    }

    public DirectoryDto getDirectoryDto() {
        return directoryDto;
    }

    public SeriesDetail getSeriesDetail() {
        return seriesDetail;
    }

    public Map<Integer, Episode[]> getEpisodesBySeasonNumber() {
        return episodesBySeasonNumber;
    }

    public Map<Integer, DirectoryDto> getSeasonDirectoriesBySeasonNumber() {
        return seasonDirectoriesBySeasonNumber;
    }

    public List<SeasonEntry> getSeasonEntryList() {
        return seasonEntryList;
    }
}
