package ch.andreskonrad.torenta.tmdb.dto;

import java.util.Objects;

public class Season {

    private String air_date;
    private int episode_count;
    private int id;
    private String _id;
    private String name;
    private String overview;
    private String poster_path;
    private int season_number;
    private Episode[] episodes;

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

    public Episode[] getEpisodes() {
        return episodes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Season season = (Season) o;
        return id == season.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
