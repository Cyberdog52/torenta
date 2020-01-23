package ch.andreskonrad.torenta.library.dto;

import ch.andreskonrad.torenta.directory.dto.DirectoryDto;
import ch.andreskonrad.torenta.tmdb.dto.Episode;
import ch.andreskonrad.torenta.tmdb.dto.SeriesDetail;

import java.util.Map;

public class SeriesEntry {
    private final DirectoryDto directoryDto;
    private final SeriesDetail seriesDetail;
    private final Map<Integer, Episode[]> episodesBySeasonNumber;


    public SeriesEntry(DirectoryDto seriesDirectoryDto, SeriesDetail seriesDetail, Map<Integer, Episode[]> episodesBySeasonNumber) {
        this.directoryDto = seriesDirectoryDto;
        this.seriesDetail = seriesDetail;
        this.episodesBySeasonNumber = episodesBySeasonNumber;

        //TODO make season entries and episode entries here
    }
}
