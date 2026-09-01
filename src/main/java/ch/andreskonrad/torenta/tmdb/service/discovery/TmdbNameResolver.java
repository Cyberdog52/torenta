package ch.andreskonrad.torenta.tmdb.service.discovery;

import ch.andreskonrad.torenta.concierge.dto.AiMediaType;
import ch.andreskonrad.torenta.concierge.dto.NamedFilterKey;
import ch.andreskonrad.torenta.concierge.service.GenreIdMapper;
import ch.andreskonrad.torenta.tmdb.dto.*;
import ch.andreskonrad.torenta.tmdb.service.TmdbService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.function.Function;

@Component
public class TmdbNameResolver {

    private final TmdbService tmdbService;
    private final GenreIdMapper genreIdMapper;

    @Autowired
    public TmdbNameResolver(TmdbService tmdbService, GenreIdMapper genreIdMapper) {
        this.tmdbService = tmdbService;
        this.genreIdMapper = genreIdMapper;
    }

    public OptionalInt resolve(
            NamedFilterKey key,
            String name,
            String evidence,
            AiMediaType mediaType,
            String watchRegion
    ) {
        return switch (key) {
            case GENRE -> genreIdMapper.genreId(mediaType, name);
            case CAST, CREW, PEOPLE -> person(name);
            case COMPANY -> entity(tmdbService.searchCompanies(name), name);
            case KEYWORD -> entity(tmdbService.searchKeywords(name), name);
            case WATCH_PROVIDER -> provider(name, mediaType, watchRegion);
            case NETWORK -> numericNetworkId(name, evidence);
        };
    }

    private OptionalInt person(String name) {
        TmdbPersonSearchResultDto response = tmdbService.searchPeople(name);
        if (response == null || response.getResults() == null) {
            return OptionalInt.empty();
        }
        return matchingId(
                response.getResults(),
                TmdbPersonDto::getName,
                TmdbPersonDto::getId,
                name,
                true
        );
    }

    private OptionalInt entity(TmdbNamedEntitySearchResultDto response, String name) {
        if (response == null || response.getResults() == null) {
            return OptionalInt.empty();
        }
        return matchingId(
                response.getResults(),
                TmdbNamedEntityDto::getName,
                TmdbNamedEntityDto::getId,
                name,
                true
        );
    }

    private OptionalInt provider(
            String name,
            AiMediaType mediaType,
            String watchRegion
    ) {
        if (watchRegion == null) {
            return OptionalInt.empty();
        }
        TmdbWatchProvidersDto response = tmdbService.getWatchProviders(mediaType, watchRegion);
        if (response == null || response.getResults() == null) {
            return OptionalInt.empty();
        }
        return matchingId(
                response.getResults(),
                TmdbWatchProviderDto::getProvider_name,
                TmdbWatchProviderDto::getProvider_id,
                name,
                false
        );
    }

    private OptionalInt numericNetworkId(String name, String evidence) {
        if (!name.matches("[1-9]\\d{0,9}")
                || !normalize(evidence).contains(normalize(name))) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Integer.parseInt(name));
        } catch (NumberFormatException exception) {
            return OptionalInt.empty();
        }
    }

    private <T> OptionalInt matchingId(
            List<T> values,
            Function<T, String> name,
            java.util.function.ToIntFunction<T> id,
            String requested,
            boolean fallbackToFirstResult
    ) {
        String normalizedRequested = normalize(requested);
        var exactMatch = values.stream()
                .filter(value -> normalize(name.apply(value)).equals(normalizedRequested))
                .findFirst();
        return exactMatch
                .or(() -> fallbackToFirstResult ? values.stream().findFirst() : java.util.Optional.empty())
                .map(value -> OptionalInt.of(id.applyAsInt(value)))
                .orElseGet(OptionalInt::empty);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim();
    }
}
