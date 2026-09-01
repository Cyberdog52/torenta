package ch.andreskonrad.torenta.bittorrent.service;

/** Thrown when a lifecycle operation targets a stable download ID that is not known. */
public class DownloadNotFoundException extends RuntimeException {

    public DownloadNotFoundException(String id) {
        super("No download found with id: " + id);
    }
}
