package ch.andreskonrad.torenta.piratebay.api;

public enum PirateBayQueryOrder {

    ByName(1), 
    ByNameDescending(2),
    ByUploaded(3),
    ByUploadedDescending(4),
    BySize(5),
    BySizeDescending(6),
    BySeeds(7),
    BySeedsDescending(8),
    ByLeechers(9),
    ByLeechersDescending(10),
    ByUledBy(11),
    ByUledByDescending(12),
    ByType(13),
    ByTypeDescending(14),
    ByDefault(99);
    
    private final int value;
    
    private PirateBayQueryOrder(int value) {
    
    	this.value = value;
    }
    
    public int getValue() {
    	
    	return this.value;
    }
}