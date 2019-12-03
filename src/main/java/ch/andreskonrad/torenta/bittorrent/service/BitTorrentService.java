package ch.andreskonrad.torenta.bittorrent.service;

import bt.Bt;
import bt.data.file.FileSystemStorage;
import bt.runtime.BtClient;
import bt.runtime.Config;
import bt.torrent.TorrentSessionState;
import ch.andreskonrad.torenta.bittorrent.dto.Download;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class BitTorrentService {

    private final int SESSION_STATE_UPDATE_INTERVAL = 1000; //in ms
    private static List<Download> downloads = new ArrayList<>();

    public int startDownload(String magnetLink, Path targetDirectory) throws IllegalStateException {
        int id = generateId(magnetLink);

        BtClient client = Bt.client()
                .config(getConfig())
                .storage(new FileSystemStorage(targetDirectory))
                .magnet(magnetLink)
                .autoLoadModules()
                .stopWhenDownloaded()
                .build();

        CompletableFuture torrentFuture = client.startAsync(
                torrentSessionState -> processSessionState(torrentSessionState, id),
                SESSION_STATE_UPDATE_INTERVAL);
        downloads.add(new Download(id, client, torrentFuture));
        return id;
    }

    public Set<Integer> getTorrentIds() {
        return downloads.stream()
                .map(Download::getId)
                .collect(Collectors.toSet());
    }

    public TorrentSessionState getState(int id) {
        return getDownload(id).getState();
    }

    private synchronized int generateId(String magnetLink) {
        int id = magnetLink.hashCode();
        if (getDownload(id) != null) {
            throw new IllegalStateException("Already downloading a torrent with magnet link: " + magnetLink);
        }
        return id;
    }

    private static void processSessionState(TorrentSessionState state, int id) {
        getDownload(id).setState(state);
    }

    private static Download getDownload(int id) {
        return downloads.stream()
                .filter(download -> download.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public void startDownloadToDownloadsFolder(String magnetLink) {
        Path pathTodownloadsFolder = Paths.get(System.getProperty("user.home"), "Downloads");
        startDownload(magnetLink, pathTodownloadsFolder);
    }

    private Config getConfig() {
        return new Config() {
            @Override
            public int getNumOfHashingThreads() {
                return Runtime.getRuntime().availableProcessors() * 2;
            }
        };
    }

}
