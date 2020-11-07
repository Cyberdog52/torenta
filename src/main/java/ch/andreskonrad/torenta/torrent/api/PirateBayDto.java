package ch.andreskonrad.torenta.torrent.api;

import ch.andreskonrad.torenta.torrent.dto.TorrentEntry;
import ch.andreskonrad.torenta.torrent.dto.TorrentSearchBuilder;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.Objects;

public class PirateBayDto {

    private long id;
    private String name;
    private String info_hash;
    private int leechers;
    private int seeders;
    private int num_files;
    private long size;
    private String username;
    private long added;
    private String status;
    private int category;
    private String imdb;

    // for jackson
    public PirateBayDto() {
    }

    public PirateBayDto(long id, String name, String info_hash, int leechers, int seeders, int num_files, long size, String username, int added, String status, int category, String imdb) {
        this.id = id;
        this.name = name;
        this.info_hash = info_hash;
        this.leechers = leechers;
        this.seeders = seeders;
        this.num_files = num_files;
        this.size = size;
        this.username = username;
        this.added = added;
        this.status = status;
        this.category = category;
        this.imdb = imdb;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInfo_hash() {
        return info_hash;
    }

    public void setInfo_hash(String info_hash) {
        this.info_hash = info_hash;
    }

    public int getLeechers() {
        return leechers;
    }

    public void setLeechers(int leechers) {
        this.leechers = leechers;
    }

    public int getSeeders() {
        return seeders;
    }

    public void setSeeders(int seeders) {
        this.seeders = seeders;
    }

    public int getNum_files() {
        return num_files;
    }

    public void setNum_files(int num_files) {
        this.num_files = num_files;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getAdded() {
        return added;
    }

    public void setAdded(long added) {
        this.added = added;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCategory() {
        return category;
    }

    public void setCategory(int category) {
        this.category = category;
    }

    public String getImdb() {
        return imdb;
    }

    public void setImdb(String imdb) {
        this.imdb = imdb;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PirateBayDto that = (PirateBayDto) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public TorrentEntry toTorrentEntry() {
        final TorrentSearchBuilder builder = new TorrentSearchBuilder();
        builder.setName(this.name);
        builder.setNumberOfSeeders(this.seeders);
        builder.setNumberOfLeechers(this.leechers);
        builder.setUploader(this.username);
        builder.setSize(getSizeString());
        builder.setUploadedTime(getPrettyTime());
        builder.setUploaderIsVIP(this.status.equalsIgnoreCase("vip"));
        builder.setMagnetLink(createMagnetURI().toString().replace("https://tpb.party/search/", ""));
        return builder.createTorrentEntry();
    }



    private URI createMagnetURI() {
        try {
            URIBuilder uriBuilder = new URIBuilder("https://tpb.party/search/magnet:");
            uriBuilder.addParameter("xt", "urn:btih:" + this.info_hash);
            uriBuilder.addParameter("dn", this.name);
            for (String tracker : TorrentSearchConstants.trackerList) {
                uriBuilder.addParameter("tr", tracker);
            }
            return uriBuilder.build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getPrettyTime() {
        Instant instant = Instant.ofEpochSecond(added);
        return instant.toString();

    }

    private String getSizeString() {
        double k = size/1024.0;
        double m = size/1048576.0;
        double g = size/1073741824.0;
        String sizeString;
        DecimalFormat dec = new DecimalFormat(" 0.00");
        if (g > 1) {
            sizeString = dec.format(g).concat(" GB");
        } else if (m > 1) {
            sizeString = dec.format(m).concat(" MB");
        } else if (k > 1) {
            sizeString = dec.format(k).concat(" KB");
        } else {
            sizeString = dec.format(size).concat(" mB");
        }
        return sizeString;
    }
}
