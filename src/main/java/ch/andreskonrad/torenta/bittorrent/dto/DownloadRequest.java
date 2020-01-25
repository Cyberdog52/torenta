package ch.andreskonrad.torenta.bittorrent.dto;

import ch.andreskonrad.torenta.piratebay.dto.PirateBayEntry;
import ch.andreskonrad.torenta.tmdb.dto.TmdbEpisodeDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSeriesDetailDto;

import java.util.Objects;

public class DownloadRequest {

    private TmdbEpisodeDto tmdbEpisodeDto;
    private PirateBayEntry pirateBayEntry;
    private TmdbSeriesDetailDto seriesDetail;

    //used for jackson
    public DownloadRequest() {
    }

    public TmdbEpisodeDto getTmdbEpisodeDto() {
        return tmdbEpisodeDto;
    }

    public PirateBayEntry getPirateBayEntry() {
        return pirateBayEntry;
    }

    public TmdbSeriesDetailDto getSeriesDetail() {
        return seriesDetail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DownloadRequest that = (DownloadRequest) o;
        return Objects.equals(tmdbEpisodeDto, that.tmdbEpisodeDto) &&
                Objects.equals(pirateBayEntry, that.pirateBayEntry) &&
                Objects.equals(seriesDetail, that.seriesDetail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tmdbEpisodeDto, pirateBayEntry, seriesDetail);
    }
}
