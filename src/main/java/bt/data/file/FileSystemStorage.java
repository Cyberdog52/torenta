/*
 * Copyright (c) 2016—2017 Andrei Tomashpolskiy and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bt.data.file;

import bt.data.StorageUnit;
import bt.data.Storage;
import bt.metainfo.Torrent;
import bt.metainfo.TorrentFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Provides file-system based storage for torrent files.
 *
 * <p>Information about the collection of files in a torrent is stored as a list of strings in the torrent metainfo.
 * This makes it possible for malicious parties to create torrents with malformed pathnames,
 * e.g. by using relative paths (. and ..), empty and invalid directory and file names, etc.
 * In the worst case this can lead to loss and corruption of user data and execution of arbitrary code.
 *
 * <p>This storage implementation performs normalization of file paths, ensuring that:
 * <ul>
 *     <li>all torrent files are stored inside the root directory of this storage (see {@link #FileSystemStorage(File)})</li>
 *     <li>all individual paths are checked for potential issues and fixed in a consistent way (see below)</li>
 * </ul>
 *
 * <p><b>Algorithm for resolving paths:</b><br>
 * 1) The following transformations are applied to each individual path element:
 * <ul>
 *     <li>trimming whitespaces,</li>
 *     <li>truncating trailing dots and whitespaces recursively,</li>
 *     <li>substituting empty names with an underscore character
 *         (this also includes names that became empty after the truncation of whitespaces and dots),</li>
 *     <li>in case there is a leading or trailing file separator,
 *         it is assumed that the path starts or ends with an empty name, respectively.</li>
 * </ul><br>
 * 2) Normalized path elements are concatenated together using {@link File#separator} as the delimiter.
 *
 * <p><b>Examples:</b><br>
 * {@code "a/b/c"     => "a/b/c"}<br>
 * {@code " a/  b /c" => "a/b/c"}<br>
 * {@code ".a/.b"     => ".a/.b"}<br>
 * {@code "a./.b."    => "a/.b"}<br>
 * {@code ""          => "_"}<br>
 * {@code "a//b"      => "a/_/b"}<br>
 * {@code "."         => "_"}<br>
 * {@code ".."        => "_"}<br>
 * {@code "/"         => "_/_"}<br>
 * {@code "/a/b/c"    => "_/a/b/c"}<br>
 *
 * @since 1.0
 */
public class FileSystemStorage implements Storage {

    private final Path rootDirectory;
    private final PathNormalizer pathNormalizer;
    private final FileSystemResumeData resumeData;

    /**
     * Create a file-system based storage inside a given directory.
     *
     * @param rootDirectory Root directory for this storage. All torrent files will be stored inside this directory.
     * @since 1.0
     */
    public FileSystemStorage(File rootDirectory) {
        this(rootDirectory.toPath());
    }

    public FileSystemStorage(Path rootDirectory) {
        this(rootDirectory, null);
    }

    public FileSystemStorage(Path rootDirectory, FileSystemResumeData resumeData) {
        this.rootDirectory = rootDirectory;
        this.pathNormalizer = new PathNormalizer(rootDirectory.getFileSystem());
        this.resumeData = resumeData;
    }

    @Override
    public StorageUnit getUnit(Torrent torrent, TorrentFile torrentFile) {
        return new FileSystemStorageUnit(resolveFile(torrent, torrentFile));
    }

    private ResolvedFile resolveFile(Torrent torrent, TorrentFile torrentFile) {
        Path torrentDirectory;
        if (torrent.getFiles().size() == 1) {
            torrentDirectory = rootDirectory;
        } else {
            String normalizedName = pathNormalizer.normalize(torrent.getName());
            torrentDirectory = rootDirectory.resolve(normalizedName);
        }
        String normalizedPath = pathNormalizer.normalize(torrentFile.getPathElements());
        return new ResolvedFile(torrentDirectory, normalizedPath, torrentFile.getSize());
    }

    @Override
    public Set<Integer> getVerifiedPieces(Torrent torrent) {
        if (resumeData == null || !torrent.getTorrentId().toString().equals(resumeData.torrentId())) {
            return Set.of();
        }
        long size = torrent.getSize();
        long chunkSize = torrent.getChunkSize();
        if (size < 0 || chunkSize <= 0) {
            return Set.of();
        }
        long pieces = size == 0 ? 0 : 1 + (size - 1) / chunkSize;
        if (pieces > Integer.MAX_VALUE) {
            return Set.of();
        }
        int piecesTotal = (int) pieces;
        if (piecesTotal != resumeData.piecesTotal()) {
            return Set.of();
        }
        Map<String, FileSystemResumeData.FileSnapshot> snapshots = new HashMap<>();
        for (FileSystemResumeData.FileSnapshot snapshot : resumeData.files()) {
            if (snapshots.put(snapshot.relativePath(), snapshot) != null) {
                return Set.of();
            }
        }
        if (snapshots.size() != torrent.getFiles().size()) {
            return Set.of();
        }
        try {
            for (TorrentFile torrentFile : torrent.getFiles()) {
                ResolvedFile resolved = resolveFile(torrent, torrentFile);
                Path file = resolved.root().resolve(resolved.path());
                String relativePath = portableRelativePath(file);
                FileSystemResumeData.FileSnapshot snapshot = snapshots.get(relativePath);
                if (snapshot == null || snapshot.fileKey() == null) {
                    return Set.of();
                }
                BasicFileAttributes attributes = Files.readAttributes(
                        file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                if (!attributes.isRegularFile()
                        || attributes.size() != torrentFile.getSize()
                        || attributes.size() != snapshot.size()
                        || attributes.lastModifiedTime().to(TimeUnit.NANOSECONDS) != snapshot.lastModifiedNanos()
                        || attributes.fileKey() == null
                        || !attributes.fileKey().toString().equals(snapshot.fileKey())) {
                    return Set.of();
                }
            }
        } catch (IOException | RuntimeException exception) {
            return Set.of();
        }
        Set<Integer> verified = new HashSet<>();
        for (Integer piece : resumeData.verifiedPieces()) {
            if (piece == null || piece < 0 || piece >= piecesTotal || !verified.add(piece)) {
                return Set.of();
            }
        }
        return Set.copyOf(verified);
    }

    private String portableRelativePath(Path file) {
        Path relative = rootDirectory.toAbsolutePath().normalize()
                .relativize(file.toAbsolutePath().normalize());
        StringBuilder result = new StringBuilder();
        for (Path element : relative) {
            if (!result.isEmpty()) {
                result.append('/');
            }
            result.append(element);
        }
        return result.toString();
    }

    record ResolvedFile(Path root, String path, long capacity) {
    }
}
