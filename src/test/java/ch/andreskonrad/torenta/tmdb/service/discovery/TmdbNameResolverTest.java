package ch.andreskonrad.torenta.tmdb.service.discovery;

import ch.andreskonrad.torenta.concierge.dto.AiMediaType;
import ch.andreskonrad.torenta.concierge.dto.NamedFilterKey;
import ch.andreskonrad.torenta.concierge.service.GenreIdMapper;
import ch.andreskonrad.torenta.tmdb.dto.TmdbNamedEntitySearchResultDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbPersonSearchResultDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbWatchProvidersDto;
import ch.andreskonrad.torenta.tmdb.service.TmdbService;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TmdbNameResolverTest {

    private final TmdbService tmdbService = mock(TmdbService.class);
    private final TmdbNameResolver resolver =
            new TmdbNameResolver(tmdbService, new GenreIdMapper());
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void resolvesExactPeopleCompaniesKeywordsProvidersAndGenres() {
        when(tmdbService.searchPeople("Jack Nicholson")).thenReturn(objectMapper.readValue(
                """
                        {"results":[{"id":1,"name":"Other"},{"id":514,"name":"Jack Nicholson"}]}
                        """,
                TmdbPersonSearchResultDto.class
        ));
        when(tmdbService.searchCompanies("Pixar")).thenReturn(namedResults(3, "Pixar"));
        when(tmdbService.searchKeywords("space")).thenReturn(namedResults(4, "space"));
        when(tmdbService.getWatchProviders(AiMediaType.MOVIE, "US"))
                .thenReturn(objectMapper.readValue(
                        """
                                {"results":[{"provider_id":8,"provider_name":"Netflix"}]}
                                """,
                        TmdbWatchProvidersDto.class
                ));

        assertEquals(514, resolved(NamedFilterKey.CAST, "Jack Nicholson", "Jack Nicholson"));
        assertEquals(3, resolved(NamedFilterKey.COMPANY, "Pixar", "Pixar"));
        assertEquals(4, resolved(NamedFilterKey.KEYWORD, "space", "space"));
        assertEquals(8, resolver.resolve(
                NamedFilterKey.WATCH_PROVIDER, "Netflix", "Netflix",
                AiMediaType.MOVIE, "US"
        ).orElseThrow());
        assertEquals(878, resolved(NamedFilterKey.GENRE, "Science Fiction", "science fiction"));
    }

    @Test
    void unresolvedAndUnevidencedNetworkIdsStayOutOfDiscover() {
        when(tmdbService.searchCompanies("Missing")).thenReturn(namedResults(0, null));

        assertTrue(resolver.resolve(
                NamedFilterKey.COMPANY, "Missing", "Missing",
                AiMediaType.MOVIE, null
        ).isEmpty());
        assertTrue(resolver.resolve(
                NamedFilterKey.NETWORK, "49", "HBO",
                AiMediaType.SERIES, null
        ).isEmpty());
        assertEquals(49, resolver.resolve(
                NamedFilterKey.NETWORK, "49", "network 49",
                AiMediaType.SERIES, null
        ).orElseThrow());
    }

    @Test
    void watchProviderRequiresExactNormalizedMatch() {
        when(tmdbService.getWatchProviders(AiMediaType.MOVIE, "US"))
                .thenReturn(objectMapper.readValue(
                        """
                                {"results":[
                                    {"provider_id":8,"provider_name":"Netflix"},
                                    {"provider_id":337,"provider_name":"Disney+"}
                                ]}
                                """,
                        TmdbWatchProvidersDto.class
                ));

        assertTrue(resolver.resolve(
                NamedFilterKey.WATCH_PROVIDER, "Amazon", "Amazon",
                AiMediaType.MOVIE, "US"
        ).isEmpty());
    }

    @Test
    void handlesMissingResponsesAndUsesOnlyDocumentedFallbacks() {
        when(tmdbService.searchPeople("Fallback")).thenReturn(objectMapper.readValue(
                """
                        {"results":[{"id":7,"name":"Different Person"}]}
                        """,
                TmdbPersonSearchResultDto.class
        ));
        when(tmdbService.searchCompanies("Fallback")).thenReturn(namedResults(8, "Different Company"));
        when(tmdbService.searchKeywords("Missing")).thenReturn(null);
        when(tmdbService.getWatchProviders(AiMediaType.MOVIE, "US")).thenReturn(null);

        assertEquals(7, resolved(NamedFilterKey.CAST, "Fallback", "Fallback"));
        assertEquals(8, resolved(NamedFilterKey.COMPANY, "Fallback", "Fallback"));
        assertTrue(resolvedOptional(NamedFilterKey.KEYWORD, "Missing", "Missing", null).isEmpty());
        assertTrue(resolvedOptional(
                NamedFilterKey.WATCH_PROVIDER, "Netflix", "Netflix", null
        ).isEmpty());
        assertTrue(resolvedOptional(
                NamedFilterKey.WATCH_PROVIDER, "Netflix", "Netflix", "US"
        ).isEmpty());
    }

    @Test
    void normalizesPunctuationAndRejectsInvalidOrOverflowingNetworkIds() {
        when(tmdbService.searchPeople("Jean Luc")).thenReturn(objectMapper.readValue(
                """
                        {"results":[{"id":9,"name":"Jean-Luc"}]}
                        """,
                TmdbPersonSearchResultDto.class
        ));

        assertEquals(9, resolved(NamedFilterKey.PEOPLE, "Jean Luc", "Jean Luc"));
        assertTrue(resolvedOptional(NamedFilterKey.NETWORK, "0", "network 0", null).isEmpty());
        assertTrue(resolvedOptional(
                NamedFilterKey.NETWORK, "9999999999", "network 9999999999", null
        ).isEmpty());
    }

    private int resolved(NamedFilterKey key, String name, String evidence) {
        return resolver.resolve(key, name, evidence, AiMediaType.MOVIE, null).orElseThrow();
    }

    private java.util.OptionalInt resolvedOptional(
            NamedFilterKey key,
            String name,
            String evidence,
            String watchRegion
    ) {
        return resolver.resolve(key, name, evidence, AiMediaType.MOVIE, watchRegion);
    }

    private TmdbNamedEntitySearchResultDto namedResults(int id, String name)  {
        String json = name == null ? "{\"results\":[]}"
                : "{\"results\":[{\"id\":" + id + ",\"name\":\"" + name + "\"}]}";
        return objectMapper.readValue(json, TmdbNamedEntitySearchResultDto.class);
    }
}
