package ch.andreskonrad.torenta.bittorrent.dto;

/**
 * Distinguishes why a {@link DownloadState#FAILED} download failed so the UI never has to infer
 * an unsafe action from a generic failed state.
 */
public enum DownloadFailureKind {
    /** The torrent engine or finalization failed but staged data is intact; restart is safe. */
    RESTARTABLE,
    /** The record or its files are unusable (invalid metadata, missing finished payload, or a
     * cleanup that could not fully complete); only cleanup/retry-delete is offered. */
    CLEANUP_ONLY
}
