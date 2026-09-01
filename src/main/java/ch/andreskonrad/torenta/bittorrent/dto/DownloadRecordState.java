package ch.andreskonrad.torenta.bittorrent.dto;

/** Lifecycle state of a durable download record on disk. */
public enum DownloadRecordState {
    STARTED, PAUSED, FINISHED, FAILED
}
