package ch.andreskonrad.torenta.bittorrent.dto;

public class DownloadDto {

    private final int id;
    private final DownloadState state;
    private final double progress;
    private final String magnetLink;

    public DownloadDto(int id, DownloadState state, double progress, String magnetLink) {
        this.id = id;
        this.state = state;
        this.progress = progress;
        this.magnetLink = magnetLink;
    }


    public double getProgress() {
        return progress;
    }

    public int getId() {
        return id;
    }

    public DownloadState getState() {
        return state;
    }

    public String getMagnetLink() {
        return magnetLink;
    }
}
