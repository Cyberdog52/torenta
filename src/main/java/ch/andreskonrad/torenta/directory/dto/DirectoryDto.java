package ch.andreskonrad.torenta.directory.dto;

import java.util.Set;

public class DirectoryDto {

    private final String name;
    private final Set<FileDto> files;
    private final Set<DirectoryDto> directories;
    private final String absolutePath;

    public DirectoryDto(String name, Set<FileDto> files, Set<DirectoryDto> directories, String absolutePath) {
        this.name = name;
        this.files = files;
        this.directories = directories;
        this.absolutePath = absolutePath;
    }

    public String getName() {
        return name;
    }

    public Set<FileDto> getFiles() {
        return files;
    }

    public Set<DirectoryDto> getDirectories() {
        return directories;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }
}


