package ch.andreskonrad.torenta.tmdb.dto;

import java.util.Objects;

public class TmdbGenreDto {

    private int id;
    private String name;

    public TmdbGenreDto() {
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TmdbGenreDto genre = (TmdbGenreDto) o;
        return id == genre.id &&
                Objects.equals(name, genre.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
