package ch.andreskonrad.torenta.library.dto;

import ch.andreskonrad.torenta.bittorrent.dto.DownloadDto;
import ch.andreskonrad.torenta.directory.dto.DirectoryDto;
import ch.andreskonrad.torenta.tmdb.dto.Episode;
import ch.andreskonrad.torenta.tmdb.dto.Season;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SeasonEntry {

    private final AirStatus airStatus;
    private final DownloadStatus downloadStatus;
    private final List<EpisodeEntry> episodeEntryList = new ArrayList<>();
    private final Season season;

    public SeasonEntry(Season season, Episode[] episodes, DirectoryDto seasonDirectory, Set<DownloadDto> downloadDtoSet) {
        this.season = season;

        for (Episode episode : episodes) {
            DownloadDto episodeDownloadDto = getDownloadDtoForEpisode(downloadDtoSet, episode);
            episodeEntryList.add(new EpisodeEntry(episode, seasonDirectory, episodeDownloadDto));
        }

        this.airStatus = getSeasonAirStatus();
        this.downloadStatus = getSeasonDownloadStatus();
    }

    private DownloadDto getDownloadDtoForEpisode(Set<DownloadDto> downloadDtoSet, Episode episode) {
        DownloadDto episodeDownloadDto = null;
        for (DownloadDto downloadDto : downloadDtoSet) {
            if (downloadDto.getDownloadRequest() == null ||
                    downloadDto.getDownloadRequest().getEpisode() == null) {
                continue;
            }
            if (downloadDto.getDownloadRequest().getEpisode().equals(episode)) {
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

    public List<EpisodeEntry> getEpisodeEntryList() {
        return episodeEntryList;
    }

    public Season getSeason() {
        return season;
    }

    private AirStatus getSeasonAirStatus() {
        int numberOfAiredEpisodes = 0;
        for (EpisodeEntry episode : episodeEntryList) {
            if (episode.getAirStatus() == AirStatus.AIRED) {
                numberOfAiredEpisodes++;
            }
        }

        if (numberOfAiredEpisodes == 0) {
            return AirStatus.NOT_AIRED;
        }
        if (numberOfAiredEpisodes == episodeEntryList.size()) {
            return AirStatus.AIRED;
        }

        return AirStatus.STARTED;
    }

    private DownloadStatus getSeasonDownloadStatus() {
        int numberOfDownloadedEpisodes = 0;
        for (EpisodeEntry episode : episodeEntryList) {
            if (episode.getDownloadStatus() == DownloadStatus.DOWNLOADED) {
                numberOfDownloadedEpisodes++;
            }
        }

        if (numberOfDownloadedEpisodes == 0) {
            return DownloadStatus.NOT_DOWNLOADED;
        }
        if (numberOfDownloadedEpisodes == episodeEntryList.size()) {
            return DownloadStatus.DOWNLOADED;
        }

        return DownloadStatus.DOWNLOADING;
    }


}
