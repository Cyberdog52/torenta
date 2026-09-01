package ch.andreskonrad.torenta.recommendation.dto;

import java.util.List;

public class SeriesRecommendationDto {

    private final String seriesName;
    private final int tmdbSeriesId;
    private final String posterPath;
    private final List<RecommendedEpisodeDto> recommendedEpisodes;

    public SeriesRecommendationDto(
            String seriesName,
            int tmdbSeriesId,
            String posterPath,
            List<RecommendedEpisodeDto> recommendedEpisodes
    ) {
        this.seriesName = seriesName;
        this.tmdbSeriesId = tmdbSeriesId;
        this.posterPath = posterPath;
        this.recommendedEpisodes = recommendedEpisodes;
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
}
