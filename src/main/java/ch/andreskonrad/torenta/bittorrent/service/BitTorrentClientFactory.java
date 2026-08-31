package ch.andreskonrad.torenta.bittorrent.service;

import bt.runtime.BtClient;
import bt.runtime.Config;

import java.nio.file.Path;

public interface BitTorrentClientFactory {

    BtClient create(Config config, Path targetDirectory, String magnetLink);
}
