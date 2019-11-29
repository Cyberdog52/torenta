package ch.andreskonrad.torenta.tmdb.dto;

import java.util.Objects;

public class Episode {

    private String air_date;
    private int episode_number;
    private int id;
    private String name;
    private String overview;
    private String production_code;
    private int season_number;
    private int show_id;
    private String still_path;
    private double vote_average;
    private int vote_count;

    public Episode() {
    }

    public String getAir_date() {
        return air_date;
    }

    public int getEpisode_number() {
        return episode_number;
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

    public String getProduction_code() {
        return production_code;
    }

    public int getSeason_number() {
        return season_number;
    }

    public int getShow_id() {
        return show_id;
    }

    public String getStill_path() {
        return still_path;
    }

    public double getVote_average() {
        return vote_average;
    }

    public int getVote_count() {
        return vote_count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Episode episode = (Episode) o;
        return episode_number == episode.episode_number &&
                id == episode.id &&
                season_number == episode.season_number &&
                show_id == episode.show_id &&
                Double.compare(episode.vote_average, vote_average) == 0 &&
                vote_count == episode.vote_count &&
                Objects.equals(air_date, episode.air_date) &&
                Objects.equals(name, episode.name) &&
                Objects.equals(overview, episode.overview) &&
                Objects.equals(production_code, episode.production_code) &&
                Objects.equals(still_path, episode.still_path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(air_date, episode_number, id, name, overview, production_code, season_number, show_id, still_path, vote_average, vote_count);
    }
}
