package ch.andreskonrad.torenta.bittorrent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Versioned on-disk representation of a single download, persisted at
 * {@code <download-root>/.torenta/downloads/<id>/download.json}. This is the sole durable source
 * of truth for a download's lifecycle; the in-memory runtime state is only a projection of it.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
public class DownloadRecord {

    public static final int CURRENT_VERSION = 1;

    private int version;
    private String id;
    private DownloadRecordState state;
    /** Non-null only while {@link #state} is {@link DownloadRecordState#FAILED}. */
    private DownloadFailureKind failureKind;
    private DownloadRequest downloadRequest;
    /** Final media destination directory, relative to the configured download root. */
    private String finalTargetRelativePath;
    private long startTimeInMs;
    private double lastProgress;
    private long lastTotalBytes;
    /** Root-relative paths of files copied into the final destination once finished. */
    private List<String> finalPayloadManifest;
    private String errorMessage;
}
