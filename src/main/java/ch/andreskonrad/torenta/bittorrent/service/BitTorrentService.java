package ch.andreskonrad.torenta.bittorrent.service;

import bt.runtime.BtClient;
import bt.runtime.Config;
import bt.torrent.TorrentSessionState;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadDto;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadRequest;
import ch.andreskonrad.torenta.directory.service.DirectoryService;
import ch.andreskonrad.torenta.tmdb.dto.TmdbMovieDetailDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class BitTorrentService {

    private static final int SESSION_STATE_UPDATE_INTERVAL = 100;

    private final DirectoryService directoryService;
    private final BitTorrentClientFactory clientFactory;
    private final List<Download> downloads = new ArrayList<>();

    public BitTorrentService(DirectoryService directoryService) {
        this(directoryService, new DefaultBitTorrentClientFactory());
    }

    @Autowired
    public BitTorrentService(DirectoryService directoryService, BitTorrentClientFactory clientFactory) {
        this.directoryService = directoryService;
        this.clientFactory = clientFactory;
    }

    public synchronized void startDownload(DownloadRequest downloadRequest, Path targetDirectory) throws IllegalStateException {
        String magnetLink = downloadRequest.getTorrentEntry().getMagnetLink();
        int id = generateId(magnetLink);

        BtClient client = clientFactory.create(
                getConfig(),
                targetDirectory,
                magnetLink.replace(" ", "%20"));

        CompletableFuture<?> torrentFuture = client.startAsync(
                torrentSessionState -> processSessionState(torrentSessionState, id),
                SESSION_STATE_UPDATE_INTERVAL);
        downloads.add(new Download(id, downloadRequest, targetDirectory, client, torrentFuture));
    }

    public void startDownloadToPreferredFolder(DownloadRequest downloadRequest) {
        Path preferredDownloadFolder = this.directoryService.getRootDirectoryPath();
        if (downloadRequest.getSeriesDetail() != null && downloadRequest.getTmdbEpisode() != null) {
            preferredDownloadFolder = this.directoryService.createDirectoryToSaveSeries(downloadRequest.getSeriesDetail().getName(), downloadRequest.getTmdbEpisode().getSeason_number());
        }
        if (downloadRequest.getMovieDetail() != null) {
            preferredDownloadFolder = this.directoryService.createDirectoryToSaveMovie(downloadRequest.getMovieDetail().getTitle(), getReleaseYear(downloadRequest.getMovieDetail()));
        }
        startDownload(downloadRequest, preferredDownloadFolder);
    }

    private Integer getReleaseYear(TmdbMovieDetailDto movieDetailDto) {
        String releaseDate = movieDetailDto.getRelease_date();
        if (releaseDate == null) {
            return null;
        }
        try {
            return LocalDate.parse(releaseDate).getYear();
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private void processSessionState(TorrentSessionState state, int id) {
        getDownload(id).setState(state);
    }

    public Download getDownload(int id) {
        return downloads.stream()
                .filter(download -> download.getId() == id)
                .findFirst()
                .orElse(null);
    }

    private Config getConfig() {
        return new Config() {
            @Override
            public int getNumOfHashingThreads() {
                return Runtime.getRuntime().availableProcessors() * 2;
            }
        };
    }

    private synchronized int generateId(String magnetLink) {
        int id = magnetLink.hashCode();
        if (getDownload(id) != null) {
            throw new IllegalStateException("Already downloading a torrent with magnet link: " + magnetLink);
        }
        return id;
    }

    public Set<DownloadDto> getAllDownloadDtos() {
        return downloads.stream()
                .map(Download::mapToDownloadDto)
                .collect(Collectors.toSet());
    }
}
