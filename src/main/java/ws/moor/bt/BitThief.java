/*
 * BitThief - A Free Riding BitTorrent Client
 * Copyright (C) 2006 Patrick Moor <patrick@moor.ws>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 */

package ws.moor.bt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ws.moor.bt.downloader.TorrentDownload;
import ws.moor.bt.downloader.TorrentDownloadConfiguration;
import ws.moor.bt.torrent.MetaInfo;
import ws.moor.bt.torrent.MetaInfoBuilder;

import java.io.File;

public class BitThief {

  private static final Logger logger = LogManager.getLogger();

  public static void main(String[] arguments) throws Exception {
    BitThiefConfiguration bitThiefConfiguration = loadBitThiefConfiguration();

    logger.info("BitThief " + Version.getLongVersionString());

    logger.info("setting up environment");
    Environment environment = new Environment(bitThiefConfiguration);

    File torrentFile = new File("C:\\Users\\anko\\Downloads\\test.torrent");
    File targetDirectory = new File("C:\\Users\\anko\\Downloads\\");

    logger.info("reading meta info file " + torrentFile);
    MetaInfo metaInfo = MetaInfoBuilder.fromFile(torrentFile);

    TorrentDownloadConfiguration configuration =
        new TorrentDownloadConfiguration(metaInfo, targetDirectory);

    logger.info("creating torrent download");
    TorrentDownload download = new TorrentDownload(environment, configuration);

    logger.info("starting torrent download");
    download.start();
  }

  private static BitThiefConfiguration loadBitThiefConfiguration() {
    BitThiefConfiguration configuration = BitThiefConfiguration.fromPropertiesFile();
    if (configuration == null) {
      System.err.println("Error loading property file.");
      System.exit(1);
    }
    return configuration;
  }
}
