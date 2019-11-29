package piratebay.dto;

public class PirateBayEntryBuilder {
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

    public PirateBayEntryBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public PirateBayEntryBuilder setMagnetLink(String magnetLink) {
        this.magnetLink = magnetLink;
        return this;
    }

    public PirateBayEntryBuilder setLink(String link) {
        this.link = link;
        return this;
    }

    public PirateBayEntryBuilder setUploadedTime(String uploadedTime) {
        this.uploadedTime = uploadedTime;
        return this;
    }

    public PirateBayEntryBuilder setSize(String size) {
        this.size = size;
        return this;
    }

    public PirateBayEntryBuilder setUploader(String uploader) {
        this.uploader = uploader;
        return this;
    }

    public PirateBayEntryBuilder setNumberOfSeeders(int numberOfSeeders) {
        this.numberOfSeeders = numberOfSeeders;
        return this;
    }

    public PirateBayEntryBuilder setNumberOfLeechers(int numberOfLeechers) {
        this.numberOfLeechers = numberOfLeechers;
        return this;
    }

    public PirateBayEntryBuilder setCategory(String category) {
        this.category = category;
        return this;
    }

    public PirateBayEntryBuilder setSubCategory(String subCategory) {
        this.subCategory = subCategory;
        return this;
    }

    public PirateBayEntryBuilder setUploaderIsVIP(boolean uploaderIsVIP) {
        this.uploaderIsVIP = uploaderIsVIP;
        return this;
    }

    public PirateBayEntryBuilder setUploaderIsTrusted(boolean uploaderIsTrusted) {
        this.uploaderIsTrusted = uploaderIsTrusted;
        return this;
    }

    public PirateBayEntry createPiratebayEntry() {
        return new PirateBayEntry(name, magnetLink, link, uploadedTime, size, uploader, numberOfSeeders, numberOfLeechers, category, subCategory, uploaderIsVIP, uploaderIsTrusted);
    }
}