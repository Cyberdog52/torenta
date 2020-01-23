package ch.andreskonrad.torenta.library.dto;

import ch.andreskonrad.torenta.tmdb.dto.Episode;

public class EpisodeEntry {

    private final AirStatus airStatus;
    private final DownloadStatus downloadStatus;
    private final Episode episode;

    public EpisodeEntry(AirStatus airStatus, DownloadStatus downloadStatus, Episode episode) {
        this.airStatus = airStatus;
        this.downloadStatus = downloadStatus;
        this.episode = episode;
    }
}
