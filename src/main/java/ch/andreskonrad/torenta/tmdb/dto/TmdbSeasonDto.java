package ch.andreskonrad.torenta.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbSeasonDto {

    private String air_date;
    private int episode_count;
    private int id;
    private String _id;
    private String name;
    private String overview;
    private String poster_path;
    private int season_number;
    private TmdbEpisodeDto[] episodes;

    public TmdbSeasonDto() {
    }

    public String getAir_date() {
        return air_date;
    }

    public int getEpisode_count() {
        return episode_count;
    }

    public int getId() {
        return id;
    }

    public String get_id() {
        return _id;
    }

    public String getName() {
        return name;
    }

    public String getOverview() {
        return overview;
    }

    public String getPoster_path() {
        return poster_path;
    }

    public int getSeason_number() {
        return season_number;
    }

    public TmdbEpisodeDto[] getEpisodes() {
        return episodes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TmdbSeasonDto tmdbSeasonDto = (TmdbSeasonDto) o;
        return id == tmdbSeasonDto.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
