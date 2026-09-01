package ch.andreskonrad.torenta.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbNamedEntityDto {
    private int id;
    private String name;
}
