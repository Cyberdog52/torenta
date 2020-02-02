package ch.andreskonrad.torenta.library.dto;

import ch.andreskonrad.torenta.bittorrent.dto.DownloadDto;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadState;
import ch.andreskonrad.torenta.directory.dto.DirectoryDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbEpisodeDto;

import java.time.LocalDate;

public class Episode {

    private final AirStatus airStatus;
    private final DownloadStatus downloadStatus;
    private final TmdbEpisodeDto tmdbEpisodeDto;
    private final int seasonNumber;
    private final int episodeNumber;
    private final String episodeString;
    private final DownloadDto downloadDto;


    public Episode(TmdbEpisodeDto tmdbEpisodeDto, DirectoryDto seasonDirectory, DownloadDto downloadDto) {
        this.tmdbEpisodeDto = tmdbEpisodeDto;
        this.seasonNumber = tmdbEpisodeDto.getSeason_number();
        this.episodeNumber = tmdbEpisodeDto.getEpisode_number();
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

    public TmdbEpisodeDto getTmdbEpisodeDto() {
        return tmdbEpisodeDto;
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
                return DownloadStatus.DOWNLOADING;
            }
            if (downloadDto.getState() == DownloadState.CANCELLED) {
                return DownloadStatus.NOT_DOWNLOADED;
            }
        }
        return null;
    }

    private AirStatus getEpisodeAirStatus() {
        if (tmdbEpisodeDto == null || tmdbEpisodeDto.getAir_date() == null) {
            return AirStatus.NOT_AIRED;
        }
        LocalDate airTime = LocalDate.parse(tmdbEpisodeDto.getAir_date());
        if (airTime.isAfter(LocalDate.now())) {
            return AirStatus.NOT_AIRED;
        } else {
            return AirStatus.AIRED;
        }
    }
}
