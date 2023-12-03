package com.profilometer.config;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigFile {

    private String inputFolder;

    public ConfigFile() {
    }

    public ConfigFile(String inputFolder) {
        this.inputFolder = inputFolder;
    }

    @JsonIgnore
    public File getOutputFolder() {
        Path inputFolderPath = Paths.get(inputFolder);
        String inputFolderName = inputFolderPath.getFileName().toString();
        String outputFolderName = String.format("%s_%s", inputFolderName, "out");
        return inputFolderPath.getParent().resolve(outputFolderName).toFile();
    }

    public String getInputFolder() {
        return inputFolder;
    }

    public void setInputFolder(String inputFolder) {
        this.inputFolder = inputFolder;
    }
}
