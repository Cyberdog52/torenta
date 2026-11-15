package bt.data.file;

import bt.metainfo.Torrent;
import bt.metainfo.TorrentFile;
import bt.metainfo.TorrentId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileSystemStorageResumeTest {

    @TempDir
    Path root;

    @Test
    void getVerifiedPieces_acceptsUnchangedCleanSnapshot() throws Exception {
        Path payload = root.resolve("payload.bin");
        Files.write(payload, new byte[]{1, 2, 3, 4});
        Torrent torrent = torrent();
        FileSystemResumeData resumeData = resumeData(payload, torrent, List.of(0, 1));

        Set<Integer> verified = new FileSystemStorage(root, resumeData).getVerifiedPieces(torrent);

        assertEquals(Set.of(0, 1), verified);
    }

    @Test
    void getVerifiedPieces_rejectsChangedFileMetadata() throws Exception {
        Path payload = root.resolve("payload.bin");
        Files.write(payload, new byte[]{1, 2, 3, 4});
        Torrent torrent = torrent();
        FileSystemResumeData resumeData = resumeData(payload, torrent, List.of(0, 1));
        Files.setLastModifiedTime(payload, FileTime.fromMillis(
                Files.getLastModifiedTime(payload).toMillis() + 1_000));

        Set<Integer> verified = new FileSystemStorage(root, resumeData).getVerifiedPieces(torrent);

        assertTrue(verified.isEmpty());
    }

    @Test
    void getVerifiedPieces_rejectsInvalidPieceIndex() throws Exception {
        Path payload = root.resolve("payload.bin");
        Files.write(payload, new byte[]{1, 2, 3, 4});
        Torrent torrent = torrent();
        FileSystemResumeData resumeData = resumeData(payload, torrent, List.of(2));

        assertTrue(new FileSystemStorage(root, resumeData).getVerifiedPieces(torrent).isEmpty());
    }

    private Torrent torrent() {
        TorrentFile file = mock(TorrentFile.class);
        when(file.getPathElements()).thenReturn(List.of("payload.bin"));
        when(file.getSize()).thenReturn(4L);
        Torrent torrent = mock(Torrent.class);
        when(torrent.getTorrentId()).thenReturn(TorrentId.fromBytes(new byte[20]));
        when(torrent.getFiles()).thenReturn(List.of(file));
        when(torrent.getSize()).thenReturn(4L);
        when(torrent.getChunkSize()).thenReturn(2L);
        return torrent;
    }

    private FileSystemResumeData resumeData(Path payload, Torrent torrent, List<Integer> pieces) throws Exception {
        BasicFileAttributes attributes = Files.readAttributes(payload, BasicFileAttributes.class);
        return new FileSystemResumeData(
                torrent.getTorrentId().toString(),
                2,
                pieces,
                List.of(new FileSystemResumeData.FileSnapshot(
                        "payload.bin",
                        attributes.size(),
                        attributes.lastModifiedTime().to(TimeUnit.NANOSECONDS),
                        attributes.fileKey().toString())));
    }
}
