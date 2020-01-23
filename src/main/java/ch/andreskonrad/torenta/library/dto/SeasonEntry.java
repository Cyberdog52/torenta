package ch.andreskonrad.torenta.library.dto;

import ch.andreskonrad.torenta.tmdb.dto.Season;

import java.util.List;

public class SeasonEntry {

    private final AirStatus airStatus;
    private final DownloadStatus downloadStatus;
    private final List<EpisodeEntry> episodeEntryList;
    private final Season season;

    public SeasonEntry(AirStatus airStatus, DownloadStatus downloadStatus, List<EpisodeEntry> episodeEntryList, Season season) {
        this.airStatus = airStatus;
        this.downloadStatus = downloadStatus;
        this.episodeEntryList = episodeEntryList;
        this.season = season;
    }
}
