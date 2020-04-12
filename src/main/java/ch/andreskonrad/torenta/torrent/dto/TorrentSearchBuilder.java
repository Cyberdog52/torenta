package ch.andreskonrad.torenta.torrent.dto;

public class TorrentSearchBuilder {
    private String name;
    private String magnetLink;
    private String link;
    private String uploadedTime;
    private String size;
    private String uploader;
    private int numberOfSeeders;
    private int numberOfLeechers;
    private String category;
    private String subCategory;
    private boolean uploaderIsVIP;
    private boolean uploaderIsTrusted;

    public TorrentSearchBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public TorrentSearchBuilder setMagnetLink(String magnetLink) {
        this.magnetLink = magnetLink;
        return this;
    }

    public TorrentSearchBuilder setLink(String link) {
        this.link = link;
        return this;
    }

    public TorrentSearchBuilder setUploadedTime(String uploadedTime) {
        this.uploadedTime = uploadedTime;
        return this;
    }

    public TorrentSearchBuilder setSize(String size) {
        this.size = size;
        return this;
    }

    public TorrentSearchBuilder setUploader(String uploader) {
        this.uploader = uploader;
        return this;
    }

    public TorrentSearchBuilder setNumberOfSeeders(int numberOfSeeders) {
        this.numberOfSeeders = numberOfSeeders;
        return this;
    }

    public TorrentSearchBuilder setNumberOfLeechers(int numberOfLeechers) {
        this.numberOfLeechers = numberOfLeechers;
        return this;
    }

    public TorrentSearchBuilder setCategory(String category) {
        this.category = category;
        return this;
    }

    public TorrentSearchBuilder setSubCategory(String subCategory) {
        this.subCategory = subCategory;
        return this;
    }

    public TorrentSearchBuilder setUploaderIsVIP(boolean uploaderIsVIP) {
        this.uploaderIsVIP = uploaderIsVIP;
        return this;
    }

    public TorrentSearchBuilder setUploaderIsTrusted(boolean uploaderIsTrusted) {
        this.uploaderIsTrusted = uploaderIsTrusted;
        return this;
    }

    public TorrentEntry createTorrentEntry() {
        return new TorrentEntry(name, magnetLink, link, uploadedTime, size, uploader, numberOfSeeders, numberOfLeechers, category, subCategory, uploaderIsVIP, uploaderIsTrusted);
    }
}