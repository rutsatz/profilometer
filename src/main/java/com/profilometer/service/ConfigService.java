package com.profilometer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profilometer.config.ConfigFile;
import com.profilometer.config.Singleton;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigService {

    private static final ObjectMapper objectMapper = Singleton.objectMapper;
    public static ConfigFile config = loadConfig();

    public static void chooseInputFolder() {
        ConfigFile configFile = loadConfig();
        File inputFolder = new File(configFile.getInputFolder());
        if (!inputFolder.exists() || !inputFolder.isDirectory()) {
            inputFolder = getUserHome();
        }

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(inputFolder);
        directoryChooser.setTitle("Select the folder with the input images");

        File destinationFolder = directoryChooser.showDialog(new Stage());

        if (destinationFolder != null) {
            updateInputFolder(destinationFolder.toPath());
        }
    }

    private static ConfigFile loadConfig() {
        try {
            File configFileLocation = resolveConfigLocation();

            if (configFileLocation.exists()) {
                return objectMapper.readValue(configFileLocation, ConfigFile.class);
            }

            return defaultConfig();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static void updateInputFolder(Path inputFolder) {
        config.setInputFolder(inputFolder.toAbsolutePath().toString());
        saveConfig();
    }

    private static void saveConfig() {
        try {
            objectMapper.writeValue(resolveConfigLocation(), config);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static File resolveConfigLocation() {
        Path currentFolder = Paths.get(System.getProperty("user.dir"));
        return currentFolder.resolve(".config").toFile();
    }

    private static ConfigFile defaultConfig() {
        Path userHome = Paths.get(getUserHome().toURI());
        return new ConfigFile(userHome.toAbsolutePath().toString());
    }

    private static File getUserHome() {
        return SystemUtils.getUserHome();
    }
}
