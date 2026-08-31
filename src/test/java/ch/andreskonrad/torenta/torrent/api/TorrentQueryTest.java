package ch.andreskonrad.torenta.torrent.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TorrentQueryTest {

    @Test
    void backendUriEncodesQueryParameters() {
        TorrentQuery query = new TorrentQuery("L'été & 東京/2?");

        assertEquals(
                "https://apibay.org?q=L%27%C3%A9t%C3%A9+%26+%E6%9D%B1%E4%BA%AC%2F2%3F&cat=",
                query.getPirateBayBackendSearchString().toASCIIString());
    }

    @Test
    void frontendUriRemovesApostrophesAndEncodesOnePathSegment() {
        TorrentQuery query = new TorrentQuery("L'été & 東京/2?");

        assertEquals(
                "https://tpb.party/search/L%C3%A9t%C3%A9%20&%20%E6%9D%B1%E4%BA%AC%2F2%3F",
                query.getPirateBayFrontendSearchURI().toASCIIString());
    }
}
