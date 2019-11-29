package ch.andreskonrad.torenta.piratebay.api;

public class PirateBayQuery {

    private PirateBayQueryOrder order;
    private int category;
    private int page;
    private String searchString;
    private boolean searchForRecentTopResults;
    
    // constructors
    // used for the 48hr and top100 categories
    public PirateBayQuery(String term) {
    	this.searchString = term;
    	searchForRecentTopResults = true;
    }
    
    public PirateBayQuery(int category) {
    	searchString = "";
    	this.category = category;
    	this.page = 0;
    	order = PirateBayQueryOrder.ByDefault;
    	searchForRecentTopResults = false;
    }
    
    public PirateBayQuery(String term, int page) {
    	this.searchString = term;
    	this.page = page;
    	category = PirateBayCategory.All;
    	order = PirateBayQueryOrder.ByDefault;
    	searchForRecentTopResults = false;
    }
    
    public PirateBayQuery(String term, int page, int category) {
    	this.searchString = term;
    	this.page = page;
    	this.category = category;
    	order = PirateBayQueryOrder.ByDefault;
    	searchForRecentTopResults = false;
    }
    
    public PirateBayQuery(String term, int page, PirateBayQueryOrder order) {
    	this.searchString = term;
    	this.page = page;
    	category = PirateBayCategory.All;
    	this.order = order;
    	searchForRecentTopResults = false;
    }
    
    public PirateBayQuery(String term, int page, int category, PirateBayQueryOrder order) {
    	this.searchString = term;
    	this.page = page;
    	this.category = category;
    	this.order = order;
    	searchForRecentTopResults = false;
    }
    
    public String TranslateToUrl() {
    	String url;
    	
    	if (!searchForRecentTopResults) {
    		url = PirateBayConstants.Url +
    			"/search/" +
					searchString + "/" +
    			Integer.toString(page) + "/" +
    			Integer.toString(order.getValue()) + "/" +
    			Integer.toString(category);
    	}
    	else {
    		url = PirateBayConstants.Url +
    				"/top/" +
					searchString;
    	}
    	
    	return url;
    }
}
