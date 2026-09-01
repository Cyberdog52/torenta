package ch.andreskonrad.torenta.bittorrent.service;

/**
 * Thrown when an engine, finalization, or filesystem cleanup operation fails. The associated
 * download record is always left in a safe, retryable state before this is thrown.
 */
public class DownloadOperationException extends RuntimeException {

    public DownloadOperationException(String message) {
        super(message);
    }

    public DownloadOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
