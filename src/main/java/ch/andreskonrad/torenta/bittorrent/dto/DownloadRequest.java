package ch.andreskonrad.torenta.bittorrent.dto;

import ch.andreskonrad.torenta.piratebay.dto.PirateBayEntry;
import ch.andreskonrad.torenta.tmdb.dto.Episode;
import ch.andreskonrad.torenta.tmdb.dto.SeriesDetail;

import java.util.Objects;

public class DownloadRequest {

    private Episode episode;
    private PirateBayEntry pirateBayEntry;
    private SeriesDetail seriesDetail;

    //used for jackson
    public DownloadRequest() {
    }

    public Episode getEpisode() {
        return episode;
    }

    public PirateBayEntry getPirateBayEntry() {
        return pirateBayEntry;
    }

    public SeriesDetail getSeriesDetail() {
        return seriesDetail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DownloadRequest that = (DownloadRequest) o;
        return Objects.equals(episode, that.episode) &&
                Objects.equals(pirateBayEntry, that.pirateBayEntry) &&
                Objects.equals(seriesDetail, that.seriesDetail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(episode, pirateBayEntry, seriesDetail);
    }
}
