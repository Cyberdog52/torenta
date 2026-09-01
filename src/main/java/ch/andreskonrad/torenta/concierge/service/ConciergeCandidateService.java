package ch.andreskonrad.torenta.concierge.service;

import ch.andreskonrad.torenta.concierge.dto.*;
import ch.andreskonrad.torenta.tmdb.dto.*;
import ch.andreskonrad.torenta.tmdb.service.TmdbService;
import ch.andreskonrad.torenta.tmdb.service.discovery.TmdbDiscoverRequest;
import ch.andreskonrad.torenta.tmdb.service.discovery.TmdbDiscoverRequestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class ConciergeCandidateService {

    private static final int MAX_CANDIDATES = 40;
    private static final int MAX_ENRICHED_CANDIDATES = 12;

    private final TmdbService tmdbService;
    private final GenreIdMapper genreIdMapper;
    private final TmdbDiscoverRequestFactory requestFactory;

    @Autowired
    public ConciergeCandidateService(
            TmdbService tmdbService,
            GenreIdMapper genreIdMapper,
            TmdbDiscoverRequestFactory requestFactory
    ) {
        this.tmdbService = tmdbService;
        this.genreIdMapper = genreIdMapper;
        this.requestFactory = requestFactory;
    }

    public List<MediaCandidate> findCandidates(SearchIntent intent) {
        List<MediaCandidate> candidates;
        EnrichmentNeeds enrichmentNeeds;
        if (intent.similarTo() != null) {
            candidates = findSimilar(intent);
            enrichmentNeeds = enrichmentNeeds(intent, null);
        } else {
            Map<AiMediaType, TmdbDiscoverRequest> requests = new HashMap<>();
            if (includes(intent.mediaType(), AiMediaType.MOVIE)) {
                requests.put(
                        AiMediaType.MOVIE,
                        requestFactory.create(intent, AiMediaType.MOVIE)
                );
            }
            if (includes(intent.mediaType(), AiMediaType.SERIES)) {
                requests.put(
                        AiMediaType.SERIES,
                        requestFactory.create(intent, AiMediaType.SERIES)
                );
            }
            candidates = discover(requests);
            enrichmentNeeds = enrichmentNeeds(intent, requests);
        }
        Map<String, MediaCandidate> unique = new LinkedHashMap<>();
        candidates.stream()
                .filter(Objects::nonNull)
                .sorted((left, right) -> Double.compare(right.popularity(), left.popularity()))
                .forEach(candidate -> unique.putIfAbsent(candidate.candidateKey(), candidate));
        List<MediaCandidate> shortlist = unique.values().stream().limit(MAX_CANDIDATES).toList();
        return enrich(shortlist, enrichmentNeeds);
    }

    private List<MediaCandidate> discover(Map<AiMediaType, TmdbDiscoverRequest> requests) {
        List<MediaCandidate> candidates = new ArrayList<>();
        TmdbDiscoverRequest request = requests.get(AiMediaType.MOVIE);
        if (request != null) {
            TmdbMoviesSearchResultDto result = requireResult(
                    tmdbService.discoverMovies(request)
            );
            safe(result.getResults()).stream()
                    .filter(Objects::nonNull)
                    .map(this::movieCandidate)
                    .forEach(candidates::add);
        }
        request = requests.get(AiMediaType.SERIES);
        if (request != null) {
            TmdbSeriesSearchResultDto result = requireResult(
                    tmdbService.discoverSeries(request)
            );
            safe(result.getResults()).stream()
                    .filter(Objects::nonNull)
                    .map(this::seriesCandidate)
                    .forEach(candidates::add);
        }
        return candidates;
    }

    private List<MediaCandidate> findSimilar(SearchIntent intent) {
        List<MediaCandidate> candidates = new ArrayList<>();
        if (includes(intent.mediaType(), AiMediaType.MOVIE)) {
            TmdbMoviesSearchResultDto matches = requireResult(tmdbService.searchMovies(intent.similarTo()));
            List<TmdbMoviesOverviewDto> results = safe(matches.getResults());
            if (!results.isEmpty()) {
                TmdbMoviesSearchResultDto similar = requireResult(
                        tmdbService.similarMovies(results.getFirst().getId())
                );
                safe(similar.getResults()).stream()
                        .filter(Objects::nonNull)
                        .map(this::movieCandidate)
                        .filter(candidate -> matchesObjectiveFilters(candidate, intent))
                        .forEach(candidates::add);
            }
        }
        if (includes(intent.mediaType(), AiMediaType.SERIES)) {
            TmdbSeriesSearchResultDto matches = requireResult(tmdbService.searchSeries(intent.similarTo()));
            List<TmdbSeriesOverviewDto> results = safe(matches.getResults());
            if (!results.isEmpty()) {
                TmdbSeriesSearchResultDto similar = requireResult(
                        tmdbService.similarSeries(results.getFirst().getId())
                );
                safe(similar.getResults()).stream()
                        .filter(Objects::nonNull)
                        .map(this::seriesCandidate)
                        .filter(candidate -> matchesObjectiveFilters(candidate, intent))
                        .forEach(candidates::add);
            }
        }
        return candidates;
    }

    private boolean matchesObjectiveFilters(MediaCandidate candidate, SearchIntent intent) {
        for (NumericFilterCriterion criterion : intent.numericFilters()) {
            Double actual = switch (criterion.key()) {
                case YEAR, PRIMARY_RELEASE_YEAR, FIRST_AIR_DATE_YEAR -> {
                    Integer year = year(candidate.releaseDate());
                    yield year == null ? null : year.doubleValue();
                }
                case VOTE_AVERAGE -> candidate.rating();
                case VOTE_COUNT -> (double) candidate.voteCount();
                case RUNTIME -> candidate.runtime() == null ? null : candidate.runtime().doubleValue();
                case PAGE -> null;
            };
            if (actual != null && !matches(actual, criterion.operator(), criterion.value())) {
                return false;
            }
        }
        for (TextFilterCriterion criterion : intent.textFilters()) {
            if (criterion.key() == TextFilterKey.ORIGINAL_LANGUAGE
                    && candidate.originalLanguage() != null
                    && !criterion.value().equalsIgnoreCase(candidate.originalLanguage())) {
                return false;
            }
        }
        for (NamedFilterCriterion criterion : intent.namedFilters()) {
            if (criterion.key() != NamedFilterKey.GENRE) {
                continue;
            }
            Set<Integer> requested = genreIdMapper.genreIdSet(
                    candidate.mediaType(), criterion.names()
            );
            if (requested.isEmpty()) {
                continue;
            }
            boolean match = criterion.matching() == FilterMatch.ALL
                    ? candidate.genreIds().containsAll(requested)
                    : requested.stream().anyMatch(candidate.genreIds()::contains);
            if ((criterion.polarity() == FilterPolarity.INCLUDE && !match)
                    || (criterion.polarity() == FilterPolarity.EXCLUDE && match)) {
                return false;
            }
        }
        return true;
    }

    private EnrichmentNeeds enrichmentNeeds(
            SearchIntent intent,
            Map<AiMediaType, TmdbDiscoverRequest> requests
    ) {
        Set<NamedFilterKey> keys = EnumSet.noneOf(NamedFilterKey.class);
        if (requests != null) {
            requests.values().stream()
                    .flatMap(request -> request.rankingOnlyCriteria().stream())
                    .map(NamedFilterCriterion::key)
                    .forEach(keys::add);
        }
        if (intent.similarTo() != null) {
            intent.namedFilters().stream().map(NamedFilterCriterion::key).forEach(keys::add);
        }
        Set<TmdbCandidateFacet> facets = EnumSet.noneOf(TmdbCandidateFacet.class);
        if (keys.contains(NamedFilterKey.CAST)
                || keys.contains(NamedFilterKey.CREW)
                || keys.contains(NamedFilterKey.PEOPLE)) {
            facets.add(TmdbCandidateFacet.CREDITS);
        }
        if (keys.contains(NamedFilterKey.KEYWORD)) {
            facets.add(TmdbCandidateFacet.KEYWORDS);
        }
        if (keys.contains(NamedFilterKey.WATCH_PROVIDER)) {
            facets.add(TmdbCandidateFacet.WATCH_PROVIDERS);
        }
        boolean hasCertificationTerritory = intent.textFilters().stream()
                .anyMatch(criterion -> criterion.key() == TextFilterKey.CERTIFICATION_COUNTRY
                        || criterion.key() == TextFilterKey.REGION);
        boolean certifications = intent.textFilters().stream()
                .anyMatch(criterion -> criterion.key() == TextFilterKey.CERTIFICATION
                        || criterion.key() == TextFilterKey.CERTIFICATION_GTE
                        || criterion.key() == TextFilterKey.CERTIFICATION_LTE)
                && (intent.similarTo() != null || !hasCertificationTerritory);
        boolean runtime = intent.similarTo() != null && intent.numericFilters().stream()
                .anyMatch(criterion -> criterion.key() == NumericFilterKey.RUNTIME);
        return new EnrichmentNeeds(Set.copyOf(keys), Set.copyOf(facets), certifications, runtime);
    }

    private List<MediaCandidate> enrich(
            List<MediaCandidate> candidates,
            EnrichmentNeeds needs
    ) {
        if (!needs.required()) {
            return candidates;
        }
        int enrichmentLimit = Math.min(MAX_ENRICHED_CANDIDATES, candidates.size());
        List<MediaCandidate> enriched = new ArrayList<>(candidates.size());
        for (int index = 0; index < candidates.size(); index++) {
            MediaCandidate candidate = candidates.get(index);
            if (index < enrichmentLimit) {
                enriched.add(enrich(candidate, needs));
            } else {
                enriched.add(candidate);
            }
        }
        return List.copyOf(enriched);
    }

    private MediaCandidate enrich(MediaCandidate candidate, EnrichmentNeeds needs) {
        Set<TmdbCandidateFacet> facets = EnumSet.noneOf(TmdbCandidateFacet.class);
        facets.addAll(needs.facets());
        if (needs.certifications()) {
            facets.add(candidate.mediaType() == AiMediaType.MOVIE
                    ? TmdbCandidateFacet.CERTIFICATIONS_MOVIE
                    : TmdbCandidateFacet.CERTIFICATIONS_SERIES);
        }
        TmdbCandidateFactsDto details = tmdbService.getCandidateFacts(
                candidate.mediaType(), candidate.id(), facets
        );
        if (details == null) {
            return candidate;
        }
        Set<NamedFilterKey> known = new HashSet<>(needs.keys());
        CandidateFacts facts = new CandidateFacts(
                known,
                names(details.getGenres()),
                details.getCredits() == null ? List.of() : names(details.getCredits().getCast()),
                details.getCredits() == null ? List.of() : names(details.getCredits().getCrew()),
                names(details.getProduction_companies()),
                keywordNames(details.getKeywords()),
                names(details.getNetworks()),
                providerNames(details.getWatchProviders()),
                certifications(details)
        );
        Integer runtime = candidate.runtime();
        if (needs.runtime()) {
            if (candidate.mediaType() == AiMediaType.MOVIE && details.getRuntime() > 0) {
                runtime = details.getRuntime();
            } else if (candidate.mediaType() == AiMediaType.SERIES
                    && details.getEpisode_run_time() != null
                    && !details.getEpisode_run_time().isEmpty()) {
                runtime = details.getEpisode_run_time().getFirst();
            }
        }
        return new MediaCandidate(
                candidate.candidateKey(), candidate.mediaType(), candidate.id(), candidate.title(),
                candidate.overview(), candidate.genreIds(), candidate.releaseDate(),
                candidate.originalLanguage(), candidate.popularity(), candidate.rating(),
                candidate.voteCount(), candidate.posterPath(), runtime, facts
        );
    }

    private List<String> names(List<TmdbNamedEntityDto> values) {
        return safe(values).stream()
                .map(TmdbNamedEntityDto::getName)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<String> keywordNames(TmdbCandidateFactsDto.Keywords keywords) {
        if (keywords == null) {
            return List.of();
        }
        List<TmdbNamedEntityDto> values = keywords.getResults() != null
                ? keywords.getResults() : keywords.getKeywords();
        return names(values);
    }

    private List<String> providerNames(TmdbCandidateFactsDto.WatchProviders providers) {
        if (providers == null || providers.getResults() == null) {
            return List.of();
        }
        return providers.getResults().values().stream()
                .flatMap(region -> java.util.stream.Stream.of(
                        region.getFlatrate(), region.getFree(), region.getAds(),
                        region.getRent(), region.getBuy()
                ))
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .map(TmdbWatchProviderDto::getProvider_name)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<String> certifications(TmdbCandidateFactsDto details) {
        List<String> result = new ArrayList<>();
        if (details.getRelease_dates() != null && details.getRelease_dates().getResults() != null) {
            details.getRelease_dates().getResults().stream()
                    .filter(Objects::nonNull)
                    .flatMap(country -> safe(country.getRelease_dates()).stream())
                    .map(TmdbCandidateFactsDto.ReleaseDate::getCertification)
                    .filter(value -> value != null && !value.isBlank())
                    .forEach(result::add);
        }
        if (details.getContent_ratings() != null
                && details.getContent_ratings().getResults() != null) {
            details.getContent_ratings().getResults().stream()
                    .map(TmdbCandidateFactsDto.ContentRating::getRating)
                    .filter(value -> value != null && !value.isBlank())
                    .forEach(result::add);
        }
        return result.stream().distinct().toList();
    }

    private record EnrichmentNeeds(
            Set<NamedFilterKey> keys,
            Set<TmdbCandidateFacet> facets,
            boolean certifications,
            boolean runtime
    ) {
        boolean required() {
            return !keys.isEmpty() || certifications || runtime;
        }
    }

    private boolean matches(double actual, FilterOperator operator, double expected) {
        return switch (operator) {
            case EQ -> Double.compare(actual, expected) == 0;
            case GTE -> actual >= expected;
            case LTE -> actual <= expected;
        };
    }

    private MediaCandidate movieCandidate(TmdbMoviesOverviewDto movie) {
        return new MediaCandidate(
                key(AiMediaType.MOVIE, movie.getId()),
                AiMediaType.MOVIE,
                movie.getId(),
                firstNonBlank(movie.getTitle(), movie.getOriginal_title()),
                movie.getOverview(),
                safe(movie.getGenre_ids()),
                movie.getRelease_date(),
                movie.getOriginal_language(),
                movie.getPopularity(),
                movie.getVote_average(),
                movie.getVote_count(),
                movie.getPoster_path(),
                null
        );
    }

    private MediaCandidate seriesCandidate(TmdbSeriesOverviewDto series) {
        return new MediaCandidate(
                key(AiMediaType.SERIES, series.getId()),
                AiMediaType.SERIES,
                series.getId(),
                firstNonBlank(series.getName(), series.getOriginal_name()),
                series.getOverview(),
                safe(series.getGenre_ids()),
                series.getFirst_air_date(),
                series.getOriginal_language(),
                series.getPopularity(),
                series.getVote_average(),
                series.getVote_count(),
                series.getPoster_path(),
                null
        );
    }

    private boolean includes(AiMediaType requested, AiMediaType candidate) {
        return requested == AiMediaType.ANY || requested == candidate;
    }

    private Integer year(String date) {
        if (date == null || date.length() < 4) {
            return null;
        }
        try {
            return Integer.valueOf(date.substring(0, 4));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }

    private String key(AiMediaType mediaType, int id) {
        return mediaType + ":" + id;
    }

    private <T> T requireResult(T result) {
        if (result == null) {
            throw new IllegalStateException("TMDB request failed");
        }
        return result;
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
