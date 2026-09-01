package ch.andreskonrad.torenta.concierge.service;

import ch.andreskonrad.torenta.concierge.dto.*;
import ch.andreskonrad.torenta.concierge.service.provider.AiProvider;
import ch.andreskonrad.torenta.concierge.service.provider.AiProviderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AiConciergeService {

    public static final int MAX_PROMPT_LENGTH = 1000;
    private static final int MAX_RESULTS = 20;

    private final AiProviderFactory providerFactory;
    private final ConciergeCandidateService candidateService;
    private final SearchIntentNormalizer intentNormalizer;

    @Autowired
    public AiConciergeService(
            AiProviderFactory providerFactory,
            ConciergeCandidateService candidateService,
            SearchIntentNormalizer intentNormalizer
    ) {
        this.providerFactory = providerFactory;
        this.candidateService = candidateService;
        this.intentNormalizer = intentNormalizer;
    }

    public AiConciergeResponse search(String prompt) {
        String validatedPrompt = validatePrompt(prompt);
        AiProvider provider = providerFactory.selectedProvider();
        SearchIntent intent = intentNormalizer.normalize(
                provider.extractIntent(validatedPrompt),
                validatedPrompt
        );
        List<MediaCandidate> candidates = candidateService.findCandidates(intent);
        if (candidates.isEmpty()) {
            return new AiConciergeResponse(intent, List.of());
        }
        List<CandidateRanking> rankings = provider.rank(validatedPrompt, intent, candidates);
        return new AiConciergeResponse(intent, rankedResults(candidates, rankings));
    }

    private List<AiRankedResult> rankedResults(
            List<MediaCandidate> candidates,
            List<CandidateRanking> rankings
    ) {
        if (rankings == null) {
            throw new IllegalStateException("AI provider returned null candidate rankings");
        }
        Map<String, MediaCandidate> candidatesByKey = candidates.stream()
                .collect(java.util.stream.Collectors.toMap(
                        MediaCandidate::candidateKey,
                        candidate -> candidate
                ));
        Map<String, CandidateRanking> validRankings = new LinkedHashMap<>();
        rankings.stream()
                .filter(Objects::nonNull)
                .filter(ranking -> candidatesByKey.containsKey(ranking.candidateKey()))
                .filter(ranking -> ranking.score() > 0 && ranking.score() <= 100)
                .filter(ranking -> ranking.explanation() != null && !ranking.explanation().isBlank())
                .sorted(Comparator.comparingInt(CandidateRanking::score).reversed())
                .forEach(ranking -> validRankings.putIfAbsent(ranking.candidateKey(), ranking));
        int[] rank = {0};
        return validRankings.values().stream()
                .limit(MAX_RESULTS)
                .map(ranking -> toResult(++rank[0], candidatesByKey.get(ranking.candidateKey()), ranking))
                .toList();
    }

    private AiRankedResult toResult(
            int rank,
            MediaCandidate candidate,
            CandidateRanking ranking
    ) {
        return new AiRankedResult(
                rank,
                candidate.mediaType(),
                candidate.id(),
                candidate.title(),
                candidate.overview(),
                candidate.posterPath(),
                candidate.releaseDate(),
                candidate.rating(),
                ranking.explanation().trim()
        );
    }

    private String validatePrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("prompt must not be blank");
        }
        String trimmed = prompt.trim();
        if (trimmed.length() > MAX_PROMPT_LENGTH) {
            throw new IllegalArgumentException("prompt must not exceed " + MAX_PROMPT_LENGTH + " characters");
        }
        return trimmed;
    }

}
