package ch.andreskonrad.torenta.torrent.api;

import org.apache.http.client.utils.URIBuilder;

import java.net.URI;

public class TorrentQuery {

    private String searchString;

    
    public TorrentQuery(String term) {
    	this.searchString = term;
    }
    
    public URI getPirateBayBackendSearchString() {
        URIBuilder b;
        try {
            b = new URIBuilder(TorrentSearchConstants.PirateBayBackendURL);
            b.addParameter("q", searchString);
            b.addParameter("cat", "");
            return b.build();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public URI getPirateBayFrontendSearchURI() {
        URIBuilder b;
        try {
            b = new URIBuilder(TorrentSearchConstants.PirateBayFrontendUrl);
            b.setPathSegments("search", searchString.replace("'", ""));
            return b.build();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
