package ch.andreskonrad.torenta.bittorrent.service;

import bt.Bt;
import bt.data.file.FileSystemStorage;
import bt.runtime.BtClient;
import bt.runtime.Config;
import bt.torrent.TorrentSessionState;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

@Service
public class BitTorrentService {

    private final int SESSION_STATE_UPDATE_INTERVAL = 1000; //in ms
    private static HashMap<Integer, TorrentSessionState> btClientList = new HashMap<>();

    public int startDownload(String magnetLink, Path targetDirectory) throws IllegalStateException {
        int id = generateId(magnetLink);

        BtClient client = Bt.client()
                .config(getConfig())
                .storage(new FileSystemStorage(targetDirectory))
                .magnet(magnetLink)
                .autoLoadModules()
                .stopWhenDownloaded()
                .build();

        client.startAsync(
                torrentSessionState -> processSessionState(torrentSessionState, id),
                SESSION_STATE_UPDATE_INTERVAL);
        return id;
    }

    public TorrentSessionState getState(int id) {
       return btClientList.get(id);
    }

    private synchronized int generateId(String magnetLink) {
        int id = magnetLink.hashCode();
        if (btClientList.containsKey(id)) {
            throw new IllegalStateException("Already downloading a torrent with magnet link: " + magnetLink);
        }
        btClientList.put(id, null);
        return id;
    }

    private static void processSessionState(TorrentSessionState state, int id) {
        btClientList.put(id, state);
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
