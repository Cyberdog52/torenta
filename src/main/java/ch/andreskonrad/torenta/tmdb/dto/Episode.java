package ch.andreskonrad.torenta.tmdb.dto;

import java.util.Arrays;
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
    private Creator[] crew;
    private Creator[] guest_stars;

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

    public Creator[] getCrew() {
        return crew;
    }

    public Creator[] getGuest_stars() {
        return guest_stars;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Episode episode = (Episode) o;
        return id == episode.id &&
                show_id == episode.show_id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, show_id);
    }
}
