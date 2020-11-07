package ch.andreskonrad.torenta.torrent.api;

import org.apache.http.client.utils.URIBuilder;

import java.net.URI;

public class TorrentQuery {

    private String searchString;

    
    public TorrentQuery(String term) {
    	this.searchString = term;
    }
    
    public URI getURI() {
        URIBuilder b;
        try {
            b = new URIBuilder(TorrentSearchConstants.Url);
            b.addParameter("q", searchString);
            b.addParameter("cat", "");
            return b.build();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
