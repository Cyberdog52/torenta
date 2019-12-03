package ch.andreskonrad.torenta.bittorrent.dto;

import bt.runtime.BtClient;
import bt.torrent.TorrentSessionState;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class Download {

    private final int id;
    private final BtClient client;
    private final CompletableFuture torrentFuture;
    private TorrentSessionState state;

    public Download(int id, BtClient client, CompletableFuture torrentFuture) {
        this.id = id;
        this.client = client;
        this.torrentFuture = torrentFuture;
    }

    public int getId() {
        return id;
    }

    public BtClient getClient() {
        return client;
    }


    public void cancel() {
        //client.stop(); TODO: is this needed
        torrentFuture.cancel(true);
    }

    public boolean isDone() {
        return torrentFuture.isDone();
    }

    public TorrentSessionState getState() {
        return state;
    }

    public void setState(TorrentSessionState state) {
        this.state = state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Download download = (Download) o;
        return id == download.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
