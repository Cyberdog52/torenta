package ch.andreskonrad.torenta.bittorrent.service;

/** Thrown when a lifecycle operation is not valid for a download's current state. */
public class InvalidDownloadStateException extends RuntimeException {

    public InvalidDownloadStateException(String message) {
        super(message);
    }
}
