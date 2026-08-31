package ch.andreskonrad.torenta.torrent.api;

import ch.andreskonrad.torenta.torrent.dto.TorrentEntry;
import ch.andreskonrad.torenta.torrent.service.TorrentSearchException;

import java.util.List;

@FunctionalInterface
public interface TorrentProvider {

    List<TorrentEntry> find(TorrentQuery query) throws TorrentSearchException;
}
