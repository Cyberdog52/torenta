package ch.andreskonrad.torenta.concierge.dto;

import java.util.List;
import java.util.Set;

public record CandidateFacts(
        Set<NamedFilterKey> knownNamedFacts,
        List<String> genres,
        List<String> cast,
        List<String> crew,
        List<String> companies,
        List<String> keywords,
        List<String> networks,
        List<String> watchProviders,
        List<String> certifications
) {
    public CandidateFacts {
        knownNamedFacts = Set.copyOf(knownNamedFacts);
        genres = List.copyOf(genres);
        cast = List.copyOf(cast);
        crew = List.copyOf(crew);
        companies = List.copyOf(companies);
        keywords = List.copyOf(keywords);
        networks = List.copyOf(networks);
        watchProviders = List.copyOf(watchProviders);
        certifications = List.copyOf(certifications);
    }

    public static CandidateFacts unknown() {
        return new CandidateFacts(
                Set.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of()
        );
    }
}
