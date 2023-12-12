package com.profilometer.controller;

import com.profilometer.processor.ImageProcessor;
import com.profilometer.service.ConfigService;
import com.profilometer.ui.Window;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainController {

    @FXML
    public ListView<Path> lvInputImages;
    @FXML
    public Button btnProcess;

    @FXML
    public VBox vbImages;

    ObservableList<Path> inputImageFiles = FXCollections.observableArrayList();
    ObjectProperty<Path> selectedImageFile = new SimpleObjectProperty<>();

    @FXML
    public void initialize() {
        Window.vbImages = vbImages;

        lvInputImages.setCellFactory(listView -> renderCellWithImage());
        lvInputImages.setItems(inputImageFiles);
        lvInputImages.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        selectedImageFile.bind(lvInputImages.getSelectionModel().selectedItemProperty());

        btnProcess.disableProperty().bind(lvInputImages.getSelectionModel().selectedItemProperty().isNull());

        initializeEvents();
    }

    private void initializeEvents() {
    }

    @FXML
    public void menuFileOpen() {
        ConfigService.chooseInputFolder();
        List<Path> images = readImages(ConfigService.config.getInputFolder());
        inputImageFiles.setAll(images);
    }

    @FXML
    public void btnProcess() {
        Window.clearImages();

//        inputImageFiles.remove(0,3);
        System.out.println(selectedImageFile.get().toString());
        new ImageProcessor().process(selectedImageFile.get().toString());


//        new SmoothingRun().smooth();
    }

    private ListCell<Path> renderCellWithImage() {
        return new ListCell<>() {
            @Override
            public void updateItem(Path path, boolean empty) {
                super.updateItem(path, empty);
                final ImageView imageView = new ImageView();
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(path.getFileName().toString());
//                    imageView.setImage(new Image(path.toUri().toString(), 80, 160, true, true, true));
                    imageView.setImage(new Image(path.toUri().toString(), 160, 160, true, true, true));
                    setGraphic(imageView);
                }
            }
        };
    }

    private List<Path> readImages(String inputFolder) {
        String acceptedImages = ".jpg";
        try (Stream<Path> paths = Files.walk(Paths.get(inputFolder))) {
            return paths.filter(path -> path.toString().endsWith(acceptedImages))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
