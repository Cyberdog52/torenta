package bt.data.file;

import java.util.List;

public record FileSystemResumeData(
        String torrentId,
        int piecesTotal,
        List<Integer> verifiedPieces,
        List<FileSnapshot> files) {

    public FileSystemResumeData {
        verifiedPieces = List.copyOf(verifiedPieces);
        files = List.copyOf(files);
    }

    public record FileSnapshot(
            String relativePath,
            long size,
            long lastModifiedNanos,
            String fileKey) {
    }
}
