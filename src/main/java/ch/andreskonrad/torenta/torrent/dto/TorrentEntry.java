package ch.andreskonrad.torenta.torrent.dto;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class TorrentEntry {
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
}
