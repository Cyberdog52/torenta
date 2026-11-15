package ch.andreskonrad.torenta.bittorrent.dto;

import java.util.List;

public record DownloadResumeCheckpoint(
        String torrentId,
        int piecesTotal,
        List<Integer> verifiedPieces,
        List<DownloadResumeFile> files) {
}
