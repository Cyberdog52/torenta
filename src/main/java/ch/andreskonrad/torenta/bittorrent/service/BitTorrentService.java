package ch.andreskonrad.torenta.bittorrent.service;

import bt.runtime.BtClient;
import bt.runtime.Config;
import bt.torrent.TorrentSessionState;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadDto;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadRequest;
import ch.andreskonrad.torenta.directory.service.DirectoryService;
import ch.andreskonrad.torenta.tmdb.dto.TmdbMovieDetailDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
public class BitTorrentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BitTorrentService.class);
    private static final int SESSION_STATE_UPDATE_INTERVAL = 100;

    private final DirectoryService directoryService;
    private final BitTorrentClientFactory clientFactory;
    private final ConcurrentMap<Integer, Download> downloads = new ConcurrentHashMap<>();

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

        Download download = new Download(
                id,
                downloadRequest,
                targetDirectory,
                client,
                new CompletableFuture<>());
        downloads.put(id, download);
        try {
            CompletableFuture<?> torrentFuture = client.startAsync(
                    torrentSessionState -> processSessionState(torrentSessionState, id),
                    SESSION_STATE_UPDATE_INTERVAL);
            torrentFuture.whenComplete((result, throwable) -> handleCompletion(id, throwable));
            download.setTorrentFuture(torrentFuture);
        } catch (RuntimeException | Error exception) {
            downloads.remove(id, download);
            throw exception;
        }
    }

    private void handleCompletion(int id, Throwable throwable) {
        Download download = getDownload(id);
        if (download == null) {
            return;
        }
        if (throwable instanceof CancellationException) {
            return;
        }
        if (throwable != null) {
            LOGGER.error("Torrent download {} failed", id, throwable);
            download.fail(rootCauseMessage(throwable));
        } else if (!download.isComplete()) {
            LOGGER.error("Torrent download {} stopped before completion. The BitTorrent processing chain was "
                    + "terminated; enable DEBUG logging for the 'bt' package to see the failing stage.", id);
            download.fail("Torrent processing was terminated before the download completed.");
        }
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return message == null ? cause.getClass().getName() : cause.getClass().getSimpleName() + ": " + message;
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
        Download download = getDownload(id);
        if (download != null) {
            download.setState(state);
        } else {
            LOGGER.warn("Received session state for unknown download id {}", id);
        }
    }

    public Download getDownload(int id) {
        return downloads.get(id);
    }

    private Config getConfig() {
        Config config = new Config() {
            @Override
            public int getNumOfHashingThreads() {
                return Runtime.getRuntime().availableProcessors() * 2;
            }
        };
        RoutableAddressResolver.resolve().ifPresentOrElse(
                address -> {
                    config.setAcceptorAddress(address);
                    LOGGER.info("BitTorrent engine bound to local address {}", address.getHostAddress());
                },
                () -> LOGGER.warn("Could not determine a locally routable address; falling back to {}. "
                                + "If downloads find no peers, this interface likely has no internet route.",
                        config.getAcceptorAddress()));
        return config;
    }

    private int generateId(String magnetLink) {
        int id = magnetLink.hashCode();
        if (getDownload(id) != null) {
            throw new IllegalStateException("Already downloading a torrent with magnet link: " + magnetLink);
        }
        return id;
    }

    public Set<DownloadDto> getAllDownloadDtos() {
        return downloads.values().stream()
                .map(Download::mapToDownloadDto)
                .collect(Collectors.toSet());
    }
}
