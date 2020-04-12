package ch.andreskonrad.torenta.torrent.api;

public class TorrentQuery {

    private String searchString;

    
    public TorrentQuery(String term) {
    	this.searchString = term;
    }
    
    public String TranslateToUrl() {
    	return TorrentSearchConstants.Url +
				"/search/" +
				searchString + "/";
    }
}
