package ch.andreskonrad.torenta.library.dto;

import ch.andreskonrad.torenta.bittorrent.dto.DownloadDto;
import ch.andreskonrad.torenta.directory.dto.DirectoryDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbEpisodeDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSeasonDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Season {

    private final AirStatus airStatus;
    private final DownloadStatus downloadStatus;
    private final List<Episode> episodeList = new ArrayList<>();
    private final TmdbSeasonDto tmdbSeasonDto;
    private final int seasonNumber;

    public Season(TmdbSeasonDto tmdbSeasonDto, TmdbEpisodeDto[] tmdbEpisodeDtos, DirectoryDto seasonDirectory, Set<DownloadDto> downloadDtoSet) {
        this.tmdbSeasonDto = tmdbSeasonDto;
        this.seasonNumber = tmdbSeasonDto != null ? tmdbSeasonDto.getSeason_number() : -1;

        for (TmdbEpisodeDto tmdbEpisodeDto : tmdbEpisodeDtos) {
            DownloadDto episodeDownloadDto = getDownloadDtoForEpisode(downloadDtoSet, tmdbEpisodeDto);
            episodeList.add(new Episode(tmdbEpisodeDto, seasonDirectory, episodeDownloadDto));
        }

        this.airStatus = getSeasonAirStatus();
        this.downloadStatus = getSeasonDownloadStatus();
    }

    private DownloadDto getDownloadDtoForEpisode(Set<DownloadDto> downloadDtoSet, TmdbEpisodeDto tmdbEpisodeDto) {
        DownloadDto episodeDownloadDto = null;
        for (DownloadDto downloadDto : downloadDtoSet) {
            if (downloadDto.getDownloadRequest() == null ||
                    downloadDto.getDownloadRequest().getTmdbEpisode() == null) {
                continue;
            }
            if (downloadDto.getDownloadRequest().getTmdbEpisode().equals(tmdbEpisodeDto)) {
                episodeDownloadDto = downloadDto;
            }
        }
        return episodeDownloadDto;
    }

    public AirStatus getAirStatus() {
        return airStatus;
    }

    public DownloadStatus getDownloadStatus() {
        return downloadStatus;
    }

    public List<Episode> getEpisodeList() {
        return episodeList;
    }

    public TmdbSeasonDto getTmdbSeasonDto() {
        return tmdbSeasonDto;
    }

    private AirStatus getSeasonAirStatus() {
        int numberOfAiredEpisodes = 0;
        for (Episode episode : episodeList) {
            if (episode.getAirStatus() == AirStatus.AIRED) {
                numberOfAiredEpisodes++;
            }
        }

        if (numberOfAiredEpisodes == 0) {
            return AirStatus.NOT_AIRED;
        }
        if (numberOfAiredEpisodes == episodeList.size()) {
            return AirStatus.AIRED;
        }

        return AirStatus.STARTED;
    }

    private DownloadStatus getSeasonDownloadStatus() {
        int numberOfDownloadedEpisodes = 0;
        for (Episode episode : episodeList) {
            if (episode.getDownloadStatus() == DownloadStatus.DOWNLOADED) {
                numberOfDownloadedEpisodes++;
            }
        }

        if (numberOfDownloadedEpisodes == 0) {
            return DownloadStatus.NOT_DOWNLOADED;
        }
        if (numberOfDownloadedEpisodes == episodeList.size()) {
            return DownloadStatus.DOWNLOADED;
        }

        return DownloadStatus.DOWNLOADING;
    }


    public int getSeasonNumber() {
        return seasonNumber;
    }
}
