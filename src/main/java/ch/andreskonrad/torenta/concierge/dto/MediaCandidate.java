package ch.andreskonrad.torenta.concierge.dto;

import java.util.List;

public record MediaCandidate(
        String candidateKey,
        AiMediaType mediaType,
        int id,
        String title,
        String overview,
        List<Integer> genreIds,
        String releaseDate,
        String originalLanguage,
        double popularity,
        double rating,
        int voteCount,
        String posterPath,
        Integer runtime,
        CandidateFacts facts
) {
    public MediaCandidate(
            String candidateKey,
            AiMediaType mediaType,
            int id,
            String title,
            String overview,
            List<Integer> genreIds,
            String releaseDate,
            String originalLanguage,
            double popularity,
            double rating,
            int voteCount,
            String posterPath,
            Integer runtime
    ) {
        this(
                candidateKey, mediaType, id, title, overview, genreIds, releaseDate,
                originalLanguage, popularity, rating, voteCount, posterPath, runtime,
                CandidateFacts.unknown()
        );
    }
}
