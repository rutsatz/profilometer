package com.profilometer.config;

public class ConfigFile {

    private String inputFolder;

    public ConfigFile() {
    }

    public ConfigFile(String inputFolder) {
        this.inputFolder = inputFolder;
    }

    public String getInputFolder() {
        return inputFolder;
    }

    public void setInputFolder(String inputFolder) {
        this.inputFolder = inputFolder;
    }
}
