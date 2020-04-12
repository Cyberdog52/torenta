package ch.andreskonrad.torenta.bittorrent.service;

import bt.Bt;
import bt.data.file.FileSystemStorage;
import bt.runtime.BtClient;
import bt.runtime.Config;
import bt.torrent.TorrentSessionState;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadDto;
import ch.andreskonrad.torenta.bittorrent.dto.DownloadRequest;
import ch.andreskonrad.torenta.directory.service.DirectoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class BitTorrentService {

    private final int SESSION_STATE_UPDATE_INTERVAL = 100; //in ms
    private static List<Download> downloads = new ArrayList<>();

    private final DirectoryService directoryService;

    @Autowired
    public BitTorrentService(DirectoryService directoryService) {
        this.directoryService = directoryService;
    }

    public synchronized void startDownload(DownloadRequest downloadRequest, Path targetDirectory) throws IllegalStateException {
        String magnetLink = downloadRequest.getTorrentEntry().getMagnetLink();
        int id = generateId(magnetLink);

        BtClient client = Bt.client()
                .config(getConfig())
                .storage(new FileSystemStorage(targetDirectory))
                .magnet(magnetLink.replace(" ", "%20"))
                .autoLoadModules()
                .stopWhenDownloaded()
                .build();

        CompletableFuture torrentFuture = client.startAsync(
                torrentSessionState -> processSessionState(torrentSessionState, id),
                SESSION_STATE_UPDATE_INTERVAL);
        downloads.add(new Download(id, downloadRequest, targetDirectory, client, torrentFuture));
    }

    public void startDownloadToPreferredFolder(DownloadRequest downloadRequest) {
        Path preferredDownloadFolder = this.directoryService.getRootDirectoryPath();
        if (downloadRequest.getSeriesDetail() != null && downloadRequest.getTmdbEpisodeDto() != null) {
            preferredDownloadFolder = this.directoryService.getPathForSeason(downloadRequest.getSeriesDetail().getName(), downloadRequest.getTmdbEpisodeDto().getSeason_number());
        }
        startDownload(downloadRequest, preferredDownloadFolder);
    }

    private static void processSessionState(TorrentSessionState state, int id) {
        getDownload(id).setState(state);
    }

    public static Download getDownload(int id) {
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
