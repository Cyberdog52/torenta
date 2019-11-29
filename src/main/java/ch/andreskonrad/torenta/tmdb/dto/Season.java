package ch.andreskonrad.torenta.tmdb.dto;

import java.util.Objects;

public class Season {

    private String air_date;
    private int episode_count;
    private int id;
    private String name;
    private String overview;
    private String poster_path;
    private int season_number;

    public Season() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Season season = (Season) o;
        return episode_count == season.episode_count &&
                id == season.id &&
                season_number == season.season_number &&
                Objects.equals(air_date, season.air_date) &&
                Objects.equals(name, season.name) &&
                Objects.equals(overview, season.overview) &&
                Objects.equals(poster_path, season.poster_path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(air_date, episode_count, id, name, overview, poster_path, season_number);
    }
}
