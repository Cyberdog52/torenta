package piratebay.dto;

import java.util.Objects;

public class PirateBayEntry {
    private final String name;

    private final  String magnetLink;
    private final  String link;
    private final  String uploadedTime;
    private final  String size;
    private final  String uploader;
    private final  int numberOfSeeders;
    private final  int numberOfLeechers;
    private final  String category;
    private final  String subCategory;
    private final  boolean uploaderIsVIP;
    private final  boolean uploaderIsTrusted;

    PirateBayEntry(String name, String magnetLink, String link, String uploadedTime, String size, String uploader, int numberOfSeeders, int numberOfLeechers, String categoryParent, String subCategory, boolean uploaderIsVIP, boolean uploaderIsTrusted) {
        this.name = name;
        this.magnetLink = magnetLink;
        this.link = link;
        this.uploadedTime = uploadedTime;
        this.size = size;
        this.uploader = uploader;
        this.numberOfSeeders = numberOfSeeders;
        this.numberOfLeechers = numberOfLeechers;
        this.category = categoryParent;
        this.subCategory = subCategory;
        this.uploaderIsVIP = uploaderIsVIP;
        this.uploaderIsTrusted = uploaderIsTrusted;
    }

    public String getName() {
        return name;
    }

    public String getMagnetLink() {
        return magnetLink;
    }

    public String getLink() {
        return link;
    }

    public String getUploadedTime() {
        return uploadedTime;
    }

    public String getSize() {
        return size;
    }

    public String getUploader() {
        return uploader;
    }

    public int getNumberOfSeeders() {
        return numberOfSeeders;
    }

    public int getNumberOfLeechers() {
        return numberOfLeechers;
    }

    public String getCategory() {
        return category;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public boolean isUploaderIsVIP() {
        return uploaderIsVIP;
    }

    public boolean isUploaderIsTrusted() {
        return uploaderIsTrusted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PirateBayEntry that = (PirateBayEntry) o;
        return numberOfSeeders == that.numberOfSeeders &&
                numberOfLeechers == that.numberOfLeechers &&
                uploaderIsVIP == that.uploaderIsVIP &&
                uploaderIsTrusted == that.uploaderIsTrusted &&
                Objects.equals(name, that.name) &&
                Objects.equals(magnetLink, that.magnetLink) &&
                Objects.equals(link, that.link) &&
                Objects.equals(uploadedTime, that.uploadedTime) &&
                Objects.equals(size, that.size) &&
                Objects.equals(uploader, that.uploader) &&
                Objects.equals(category, that.category) &&
                Objects.equals(subCategory, that.subCategory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, magnetLink, link, uploadedTime, size, uploader, numberOfSeeders, numberOfLeechers, category, subCategory, uploaderIsVIP, uploaderIsTrusted);
    }

    @Override
    public String toString() {
        return "PirateBayEntry{" +
                "name='" + name + '\'' +
                ", magnetLink='" + magnetLink + '\'' +
                ", link='" + link + '\'' +
                ", uploadedTime='" + uploadedTime + '\'' +
                ", size='" + size + '\'' +
                ", uploader='" + uploader + '\'' +
                ", numberOfSeeders=" + numberOfSeeders +
                ", numberOfLeechers=" + numberOfLeechers +
                ", category='" + category + '\'' +
                ", subCategory='" + subCategory + '\'' +
                ", uploaderIsVIP=" + uploaderIsVIP +
                ", uploaderIsTrusted=" + uploaderIsTrusted +
                '}';
    }
}
