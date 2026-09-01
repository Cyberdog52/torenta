package ch.andreskonrad.torenta.concierge.service;

import ch.andreskonrad.torenta.concierge.dto.AiMediaType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenreIdMapperTest {

    private final GenreIdMapper mapper = new GenreIdMapper();

    @Test
    void mapsNormalizedMovieAndSeriesGenresAndOmitsUnknownValues() {
        assertEquals("878,53", mapper.genreIds(
                AiMediaType.MOVIE,
                List.of("science-fiction", "Thriller", "Science Fiction", "unknown")
        ));
        assertEquals(Set.of(10759, 10765), mapper.genreIdSet(
                AiMediaType.SERIES,
                List.of("action adventure", "fantasy")
        ));
    }

    @Test
    void returnsEmptyResultsForUnknownGenresAndResolvesOptionalIds() {
        assertTrue(mapper.genreIdSet(AiMediaType.MOVIE, List.of("unknown")).isEmpty());
        assertEquals(878, mapper.genreId(
                AiMediaType.MOVIE, " science fiction "
        ).orElseThrow());
        assertEquals(10765, mapper.genreId(AiMediaType.SERIES, "science_fiction").orElseThrow());
        assertTrue(mapper.genreId(AiMediaType.MOVIE, "unknown").isEmpty());
    }
}
