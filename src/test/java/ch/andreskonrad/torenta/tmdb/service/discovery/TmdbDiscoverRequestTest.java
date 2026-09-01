package ch.andreskonrad.torenta.tmdb.service.discovery;

import ch.andreskonrad.torenta.concierge.dto.AiMediaType;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TmdbDiscoverRequestTest {

    @Test
    void rejectsNonConcreteMediaTypes() {
        assertThrows(IllegalArgumentException.class, () -> request(AiMediaType.ANY, List.of()));
        assertThrows(IllegalArgumentException.class, () -> request(null, List.of()));
    }

    @Test
    void appliesMediaDefaultsAndReplacesSingleValueParameters() {
        TmdbDiscoverRequest movie = request(
                AiMediaType.MOVIE,
                List.of(
                        criterion("page", "3"),
                        criterion("sort_by", "revenue.desc"),
                        criterion("with_genres", "878"),
                        criterion("with_genres", "53")
                )
        );
        UriComponentsBuilder movieBuilder = UriComponentsBuilder.fromUriString(
                "https://example.test/discover?sort_by=old&page=9"
        );

        movie.applyTo(movieBuilder);
        String movieQuery = movieBuilder.build().encode().toUri().getRawQuery();

        assertTrue(movieQuery.contains("page=3"));
        assertTrue(movieQuery.contains("sort_by=revenue.desc"));
        assertTrue(movieQuery.contains("include_video=false"));
        assertEquals(2, countGenres(movieQuery));

        TmdbDiscoverRequest series = request(AiMediaType.SERIES, List.of());
        UriComponentsBuilder seriesBuilder = UriComponentsBuilder.fromUriString(
                "https://example.test/discover"
        );
        series.applyTo(seriesBuilder);

        assertTrue(seriesBuilder.build().toUri().getRawQuery()
                .contains("include_null_first_air_dates=false"));
    }

    @Test
    void equalityAndHashCodeReflectCompleteRequest() {
        TmdbDiscoverRequest first = request(
                AiMediaType.MOVIE, List.of(criterion("with_genres", "878"))
        );
        TmdbDiscoverRequest same = request(
                AiMediaType.MOVIE, List.of(criterion("with_genres", "878"))
        );
        TmdbDiscoverRequest differentMedia = request(
                AiMediaType.SERIES, List.of(criterion("with_genres", "878"))
        );
        TmdbDiscoverRequest differentCriteria = request(
                AiMediaType.MOVIE, List.of(criterion("with_genres", "53"))
        );

        //noinspection EqualsWithItself
        assertEquals(first, first);
        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNotEquals(null, first);
        assertNotEquals("request", first);
        assertNotEquals(first, differentMedia);
        assertNotEquals(first, differentCriteria);
        assertTrue(first.toString().contains("with_genres"));
    }

    private TmdbDiscoverRequest request(
            AiMediaType mediaType,
            List<TmdbDiscoverRequest.RenderedCriterion> criteria
    ) {
        return new TmdbDiscoverRequest(mediaType, criteria, List.of());
    }

    private TmdbDiscoverRequest.RenderedCriterion criterion(String parameter, String value) {
        return new TmdbDiscoverRequest.RenderedCriterion(parameter, value);
    }

    private long countGenres(String value) {
        return value.lines()
                .flatMap(line -> java.util.Arrays.stream(line.split("&")))
                .filter(part -> part.startsWith("with_genres="))
                .count();
    }
}
