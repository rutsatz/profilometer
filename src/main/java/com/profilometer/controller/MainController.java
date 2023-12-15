package com.profilometer.controller;

import com.profilometer.processor.ImageProcessor;
import com.profilometer.service.ConfigService;
import com.profilometer.ui.Window;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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

    @FXML
    public Spinner<Integer> spinnerImageHeight;

    @FXML
    public CheckBox ckBlurEnabled;
    @FXML
    public Spinner<Integer> spinnerBlurKernelSize;

    @FXML
    public CheckBox ckSegmentationEnabled;
    @FXML
    public Spinner<Integer> spinnerSobelKernelSize;
    @FXML
    public Spinner<Double> spinnerSegmentationThreshold;

    ObservableList<Path> inputImageFiles = FXCollections.observableArrayList();
    ObjectProperty<Path> selectedImageFile = new SimpleObjectProperty<>();
    IntegerProperty imageHeight = new SimpleIntegerProperty();

    IntegerProperty blurKernelSize = new SimpleIntegerProperty();
    BooleanProperty blurEnabled = new SimpleBooleanProperty();

    BooleanProperty segmentationEnabled = new SimpleBooleanProperty();
    IntegerProperty sobelKernelSize = new SimpleIntegerProperty();
    DoubleProperty segmentationThreshold = new SimpleDoubleProperty();

    @FXML
    public void initialize() {
        Window.vbImages = vbImages;

        lvInputImages.setCellFactory(listView -> renderCellWithImage());
        lvInputImages.setItems(inputImageFiles);
        lvInputImages.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        selectedImageFile.bind(lvInputImages.getSelectionModel().selectedItemProperty());

        btnProcess.disableProperty().bind(lvInputImages.getSelectionModel().selectedItemProperty().isNull());

        imageHeight.bind(spinnerImageHeight.valueProperty());
        spinnerImageHeight.setValueFactory(new SpinnerValueFactory
                .IntegerSpinnerValueFactory(50, 8_000, 500, 50));

        blurEnabled.bind(ckBlurEnabled.selectedProperty());
        spinnerBlurKernelSize.disableProperty().bind(blurEnabled.not());
        blurKernelSize.bind(spinnerBlurKernelSize.valueProperty());
        spinnerBlurKernelSize.setValueFactory(new SpinnerValueFactory
                .IntegerSpinnerValueFactory(3, 99, 3, 2));

        segmentationEnabled.bind(ckSegmentationEnabled.selectedProperty());
        spinnerSobelKernelSize.disableProperty().bind(segmentationEnabled.not());
        spinnerSegmentationThreshold.disableProperty().bind(segmentationEnabled.not());
        sobelKernelSize.bind(spinnerSobelKernelSize.valueProperty());
        segmentationThreshold.bind(spinnerSegmentationThreshold.valueProperty());
        spinnerSobelKernelSize.setValueFactory(new SpinnerValueFactory
                .IntegerSpinnerValueFactory(3, 7, 3, 2));
        spinnerSegmentationThreshold.setValueFactory(new SpinnerValueFactory
                .DoubleSpinnerValueFactory(1, 100, 4, 0.1));

    }

    @FXML
    public void menuFileOpen() {
        ConfigService.chooseInputFolder();
        List<Path> images = readImages(ConfigService.config.getInputFolder());
        inputImageFiles.setAll(images);
    }

    @FXML
    public void menuFileQuit() {
        System.exit(0);
    }

    @FXML
    public void btnProcess() {
        Window.clearImages();

        System.out.println(selectedImageFile.get().toString());
        new ImageProcessor().process(selectedImageFile.get().toString(), imageHeight, blurEnabled.get(), blurKernelSize.get(),
                segmentationEnabled.get(), sobelKernelSize.get(), segmentationThreshold.get());


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
