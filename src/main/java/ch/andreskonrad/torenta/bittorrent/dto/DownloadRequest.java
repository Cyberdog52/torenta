package ch.andreskonrad.torenta.bittorrent.dto;

import ch.andreskonrad.torenta.tmdb.dto.TmdbEpisodeDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbSeriesDetailDto;
import ch.andreskonrad.torenta.torrent.dto.TorrentEntry;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class DownloadRequest {
    private TmdbEpisodeDto tmdbEpisodeDto;
    private TorrentEntry torrentEntry;
    private TmdbSeriesDetailDto seriesDetail;
}
