package ch.andreskonrad.torenta.library.dto;

import ch.andreskonrad.torenta.bittorrent.dto.DownloadDto;
import ch.andreskonrad.torenta.directory.dto.DirectoryDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbEpisodeDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSeasonDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSeriesDetailDto;

import java.util.*;

public class Series {
    private final DirectoryDto directoryDto;

    private final TmdbSeriesDetailDto seriesDetail;
    private final Map<Integer, TmdbEpisodeDto[]> episodesBySeasonNumber;
    private final Map<Integer, DirectoryDto> seasonDirectoriesBySeasonNumber;
    private final List<Season> seasonList = new ArrayList<>();

    public Series(DirectoryDto seriesDirectoryDto, TmdbSeriesDetailDto seriesDetail, Map<Integer, TmdbEpisodeDto[]> episodesBySeasonNumber, HashMap<Integer, DirectoryDto> seasonDirectoriesBySeasonNumber, Set<DownloadDto> downloadDtoSet) {
        this.directoryDto = seriesDirectoryDto;
        this.seriesDetail = seriesDetail;
        this.episodesBySeasonNumber = episodesBySeasonNumber;
        this.seasonDirectoriesBySeasonNumber = seasonDirectoriesBySeasonNumber;


        for (Integer seasonNumber : episodesBySeasonNumber.keySet()) {
            DirectoryDto seasonDirectory = seasonDirectoriesBySeasonNumber.get(seasonNumber);
            TmdbEpisodeDto[] tmdbEpisodeDtos = episodesBySeasonNumber.get(seasonNumber);
            TmdbSeasonDto tmdbSeasonDto = seriesDetail.getSeasons().stream()
                    .filter(s -> s.getSeason_number() == seasonNumber).findFirst()
                    .orElse(null);
            seasonList.add(new Season(tmdbSeasonDto, tmdbEpisodeDtos, seasonDirectory, downloadDtoSet));
        }
    }

    public DirectoryDto getDirectoryDto() {
        return directoryDto;
    }

    public TmdbSeriesDetailDto getSeriesDetail() {
        return seriesDetail;
    }

    public Map<Integer, TmdbEpisodeDto[]> getEpisodesBySeasonNumber() {
        return episodesBySeasonNumber;
    }

    public Map<Integer, DirectoryDto> getSeasonDirectoriesBySeasonNumber() {
        return seasonDirectoriesBySeasonNumber;
    }

    public List<Season> getSeasonList() {
        return seasonList;
    }
}
