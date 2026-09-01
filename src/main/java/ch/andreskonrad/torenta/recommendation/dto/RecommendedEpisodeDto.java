package ch.andreskonrad.torenta.recommendation.dto;

import java.util.Objects;

public class RecommendedEpisodeDto {

    private final int seasonNumber;
    private final int episodeNumber;
    private final String episodeString;
    private final String name;
    private final String airDate;
    private final String stillPath;

    public RecommendedEpisodeDto(
            int seasonNumber,
            int episodeNumber,
            String episodeString,
            String name,
            String airDate,
            String stillPath
    ) {
        this.seasonNumber = seasonNumber;
        this.episodeNumber = episodeNumber;
        this.episodeString = episodeString;
        this.name = name;
        this.airDate = airDate;
        this.stillPath = stillPath;
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

    public String getName() {
        return name;
    }

    public String getAirDate() {
        return airDate;
    }

    public String getStillPath() {
        return stillPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecommendedEpisodeDto that = (RecommendedEpisodeDto) o;
        return seasonNumber == that.seasonNumber && episodeNumber == that.episodeNumber
                && Objects.equals(episodeString, that.episodeString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(seasonNumber, episodeNumber, episodeString);
    }
}
