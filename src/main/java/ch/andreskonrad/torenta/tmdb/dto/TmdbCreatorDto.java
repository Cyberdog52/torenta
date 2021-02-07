package ch.andreskonrad.torenta.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbCreatorDto {

    private int id;
    private String credit_id;
    private String name;
    private int gender;
    private String profile_path;
    private String department;
    private String job;
    private String character;
    private int order;
    private String adult;

    public TmdbCreatorDto() {
    }

    public int getId() {
        return id;
    }

    public String getCredit_id() {
        return credit_id;
    }

    public String getName() {
        return name;
    }

    public int getGender() {
        return gender;
    }

    public String getProfile_path() {
        return profile_path;
    }

    public String getDepartment() {
        return department;
    }

    public String getJob() {
        return job;
    }

    public String getCharacter() {
        return character;
    }

    public int getOrder() {
        return order;
    }

    public String getAdult() {
        return adult;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TmdbCreatorDto creator = (TmdbCreatorDto) o;
        return id == creator.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
