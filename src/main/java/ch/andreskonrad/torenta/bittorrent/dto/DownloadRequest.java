package ch.andreskonrad.torenta.bittorrent.dto;

import ch.andreskonrad.torenta.tmdb.dto.TmdbEpisodeDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSeriesDetailDto;
import ch.andreskonrad.torenta.torrent.dto.TorrentEntry;

import java.util.Objects;

public class DownloadRequest {

    private TmdbEpisodeDto tmdbEpisodeDto;
    private TorrentEntry torrentEntry;
    private TmdbSeriesDetailDto seriesDetail;

    //used for jackson
    public DownloadRequest() {
    }

    public TmdbEpisodeDto getTmdbEpisodeDto() {
        return tmdbEpisodeDto;
    }

    public TorrentEntry getTorrentEntry() {
        return torrentEntry;
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
                Objects.equals(torrentEntry, that.torrentEntry) &&
                Objects.equals(seriesDetail, that.seriesDetail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tmdbEpisodeDto, torrentEntry, seriesDetail);
    }
}
