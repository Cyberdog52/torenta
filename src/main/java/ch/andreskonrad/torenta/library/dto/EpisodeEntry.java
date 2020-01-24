package ch.andreskonrad.torenta.library.dto;

import ch.andreskonrad.torenta.bittorrent.dto.DownloadDto;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadState;
import ch.andreskonrad.torenta.directory.dto.DirectoryDto;
import ch.andreskonrad.torenta.tmdb.dto.Episode;

import java.time.LocalDate;

public class EpisodeEntry {

    private final AirStatus airStatus;
    private final DownloadStatus downloadStatus;
    private final Episode episode;
    private final int seasonNumber;
    private final int episodeNumber;
    private final String episodeString;
    private final DownloadDto downloadDto;


    public EpisodeEntry(Episode episode, DirectoryDto seasonDirectory, DownloadDto downloadDto) {
        this.episode = episode;
        this.seasonNumber = episode.getSeason_number();
        this.episodeNumber = episode.getEpisode_number();
        this.episodeString = createEpisodeString();
        this.airStatus = getEpisodeAirStatus();
        this.downloadDto = downloadDto;
        this.downloadStatus = getDownloadStatusFromDownloadDtoIfPresent() != null ? getDownloadStatusFromDownloadDtoIfPresent() : getEpisodeDownloadStatus(seasonDirectory);
    }

    public AirStatus getAirStatus() {
        return airStatus;
    }

    public DownloadStatus getDownloadStatus() {
        return downloadStatus;
    }

    public Episode getEpisode() {
        return episode;
    }

    public int getSeasonNumber() {
        return seasonNumber;
    }

    public int getEpisodeNumber() {
        return episodeNumber;
    }

    public String getEpisodeString() {
        return episodeString;
    }

    private String createEpisodeString() {
        StringBuilder s = new StringBuilder().append("S");
        if (seasonNumber < 10) s.append(0);
        s.append(seasonNumber);
        s.append("E");
        if (episodeNumber < 10) s.append(0);
        s.append(episodeNumber);
        return s.toString();
    }

    private DownloadStatus getEpisodeDownloadStatus(DirectoryDto directoryDto) {

        boolean directoryHasEpisode = directoryDto.getFiles()
                .stream()
                .anyMatch(f -> f.getName().toLowerCase()
                        .contains(episodeString.toLowerCase()));

        if (directoryHasEpisode) {
            return DownloadStatus.DOWNLOADED;
        }

        for (DirectoryDto subDirectory : directoryDto.getDirectories()) {
            DownloadStatus subDirectoryDownloadStatus = getEpisodeDownloadStatus(subDirectory);
            if (subDirectoryDownloadStatus.equals(DownloadStatus.DOWNLOADED)) {
                return DownloadStatus.DOWNLOADED;
            }
        }


        return DownloadStatus.NOT_DOWNLOADED;
    }

    private DownloadStatus getDownloadStatusFromDownloadDtoIfPresent() {
        if (downloadDto != null && downloadDto.getState() != null) {
            if (downloadDto.getState() == DownloadState.FINISHED) {
                return DownloadStatus.DOWNLOADED;
            }
            if (downloadDto.getState() == DownloadState.STARTED) {
                return DownloadStatus.DOWNLOADED;
            }
            if (downloadDto.getState() == DownloadState.CANCELLED) {
                return DownloadStatus.NOT_DOWNLOADED;
            }
        }
        return null;
    }

    private AirStatus getEpisodeAirStatus() {
        if (episode == null || episode.getAir_date() == null) {
            return AirStatus.NOT_AIRED;
        }
        LocalDate airTime = LocalDate.parse(episode.getAir_date());
        if (airTime.isAfter(LocalDate.now())) {
            return AirStatus.NOT_AIRED;
        } else {
            return AirStatus.AIRED;
        }
    }
}
