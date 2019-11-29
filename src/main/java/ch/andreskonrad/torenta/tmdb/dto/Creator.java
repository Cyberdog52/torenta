package ch.andreskonrad.torenta.tmdb.dto;

import java.util.Objects;

public class Creator {

    private int id;
    private String credit_id;
    private String name;
    private int gender;
    private String profile_path;

    public Creator() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Creator creator = (Creator) o;
        return id == creator.id &&
                gender == creator.gender &&
                Objects.equals(credit_id, creator.credit_id) &&
                Objects.equals(name, creator.name) &&
                Objects.equals(profile_path, creator.profile_path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, credit_id, name, gender, profile_path);
    }
}
