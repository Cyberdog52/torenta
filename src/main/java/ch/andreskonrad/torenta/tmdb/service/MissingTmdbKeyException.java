package ch.andreskonrad.torenta.tmdb.service;

/**
 * Thrown when a TMDB request cannot be made because no (valid) TMDB service key is configured.
 * This is a distinct type (rather than a plain {@link IllegalStateException}) so callers can tell
 * this systemic configuration problem apart from an ordinary "no match found" failure and react
 * to it specifically, e.g. by mapping it to {@code HttpStatus.PRECONDITION_FAILED} instead of
 * silently treating it the same as an unresolved/ambiguous lookup.
 */
public class MissingTmdbKeyException extends IllegalStateException {

    public MissingTmdbKeyException(String message) {
        super(message);
    }
}
