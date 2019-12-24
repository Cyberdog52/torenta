package ch.andreskonrad.torenta.directory.dto;

public class FileDto {

    private final String absolutePath;
    private final String name;

    public FileDto(String absolutePath, String name) {
        this.absolutePath = absolutePath;
        this.name = name;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public String getName() {
        return name;
    }
}
