package ch.andreskonrad.torenta.concierge.dto;

public record AiRankedResult(
        int rank,
        AiMediaType mediaType,
        int id,
        String title,
        String overview,
        String posterPath,
        String releaseDate,
        double rating,
        String explanation
) {
}
