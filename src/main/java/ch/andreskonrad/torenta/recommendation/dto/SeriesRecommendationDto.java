package ch.andreskonrad.torenta.recommendation.dto;

import ch.andreskonrad.torenta.tmdb.dto.TmdbSeriesDetailDto;

import java.util.List;

public class SeriesRecommendationDto {

    private final String seriesName;
    private final int tmdbSeriesId;
    private final String posterPath;
    private final List<RecommendedEpisodeDto> recommendedEpisodes;
    private final TmdbSeriesDetailDto seriesDetail;

    public SeriesRecommendationDto(
            String seriesName,
            int tmdbSeriesId,
            String posterPath,
            List<RecommendedEpisodeDto> recommendedEpisodes,
            TmdbSeriesDetailDto seriesDetail
    ) {
        this.seriesName = seriesName;
        this.tmdbSeriesId = tmdbSeriesId;
        this.posterPath = posterPath;
        this.recommendedEpisodes = recommendedEpisodes;
        this.seriesDetail = seriesDetail;
    }

    public String getSeriesName() {
        return seriesName;
    }

    public int getTmdbSeriesId() {
        return tmdbSeriesId;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public List<RecommendedEpisodeDto> getRecommendedEpisodes() {
        return recommendedEpisodes;
    }

    /**
     * The raw TMDB series detail, needed by the frontend to build a {@code DownloadRequestDto}
     * (e.g. {@code BitTorrentService} reads {@code seriesDetail.name} to resolve the download
     * folder) so a torrent can be started directly from the Recommendations page.
     */
    public TmdbSeriesDetailDto getSeriesDetail() {
        return seriesDetail;
    }
}
