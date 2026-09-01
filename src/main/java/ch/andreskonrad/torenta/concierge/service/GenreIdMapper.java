package ch.andreskonrad.torenta.concierge.service;

import ch.andreskonrad.torenta.concierge.dto.AiMediaType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class GenreIdMapper {

    private static final Map<String, Integer> MOVIE_GENRES = Map.ofEntries(
            Map.entry("ACTION", 28),
            Map.entry("ADVENTURE", 12),
            Map.entry("ANIMATION", 16),
            Map.entry("COMEDY", 35),
            Map.entry("CRIME", 80),
            Map.entry("DOCUMENTARY", 99),
            Map.entry("DRAMA", 18),
            Map.entry("FAMILY", 10751),
            Map.entry("FANTASY", 14),
            Map.entry("HISTORY", 36),
            Map.entry("HORROR", 27),
            Map.entry("MUSIC", 10402),
            Map.entry("MYSTERY", 9648),
            Map.entry("ROMANCE", 10749),
            Map.entry("SCIENCE_FICTION", 878),
            Map.entry("THRILLER", 53),
            Map.entry("WAR", 10752),
            Map.entry("WESTERN", 37)
    );

    private static final Map<String, Integer> SERIES_GENRES = Map.ofEntries(
            Map.entry("ACTION", 10759),
            Map.entry("ACTION_ADVENTURE", 10759),
            Map.entry("ADVENTURE", 10759),
            Map.entry("ANIMATION", 16),
            Map.entry("COMEDY", 35),
            Map.entry("CRIME", 80),
            Map.entry("DOCUMENTARY", 99),
            Map.entry("DRAMA", 18),
            Map.entry("FAMILY", 10751),
            Map.entry("KIDS", 10762),
            Map.entry("MYSTERY", 9648),
            Map.entry("NEWS", 10763),
            Map.entry("REALITY", 10764),
            Map.entry("SCIENCE_FICTION", 10765),
            Map.entry("FANTASY", 10765),
            Map.entry("SOAP", 10766),
            Map.entry("TALK", 10767),
            Map.entry("WAR", 10768),
            Map.entry("POLITICS", 10768),
            Map.entry("WESTERN", 37)
    );

    public String genreIds(AiMediaType mediaType, List<String> genres) {
        Map<String, Integer> mapping = mediaType == AiMediaType.MOVIE ? MOVIE_GENRES : SERIES_GENRES;
        Set<Integer> ids = new LinkedHashSet<>();
        for (String genre : genres) {
            Integer id = mapping.get(normalize(genre));
            if (id != null) {
                ids.add(id);
            }
        }
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    public Set<Integer> genreIdSet(AiMediaType mediaType, List<String> genres) {
        String ids = genreIds(mediaType, genres);
        if (ids.isEmpty()) {
            return Set.of();
        }

        return java.util.Arrays.stream(ids.split(","))
                .map(Integer::valueOf)
                .collect(Collectors.toUnmodifiableSet());
    }

    public OptionalInt genreId(AiMediaType mediaType, String genre) {
        Map<String, Integer> mapping = mediaType == AiMediaType.MOVIE ? MOVIE_GENRES : SERIES_GENRES;
        Integer id = mapping.get(normalize(genre));
        return id == null ? OptionalInt.empty() : OptionalInt.of(id);
    }

    private String normalize(String genre) {
        return genre.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }
}
