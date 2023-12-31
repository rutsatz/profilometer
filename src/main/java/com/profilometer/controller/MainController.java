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
    public Label lblAxlesRunning;
    @FXML
    public Label lblLiftedAxles;

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
    @FXML
    public Spinner<Double> spinnerVehicleFloorCutoffPercentageFix;
    @FXML
    public CheckBox ckROIBlurEnabled;
    @FXML
    public Spinner<Integer> spinnerROIBlurKernelSize;
    @FXML
    public Spinner<Integer> spinnerAspectRatioThresholdLimit;
    @FXML
    public Spinner<Integer> spinnerGapCloseSmallKernel;
    @FXML
    public Spinner<Integer> spinnerGapCloseBigKernel;
    @FXML
    public Spinner<Integer> spinnerMorphFillKernelWidth;
    @FXML
    public Spinner<Integer> spinnerMorphFillKernelHeight;

    ObservableList<Path> inputImageFiles = FXCollections.observableArrayList();
    ObjectProperty<Path> selectedImageFile = new SimpleObjectProperty<>();
    StringProperty axlesRunning = new SimpleStringProperty();
    StringProperty liftedAxles = new SimpleStringProperty();

    IntegerProperty imageHeight = new SimpleIntegerProperty();

    IntegerProperty blurKernelSize = new SimpleIntegerProperty();
    BooleanProperty blurEnabled = new SimpleBooleanProperty();

    BooleanProperty segmentationEnabled = new SimpleBooleanProperty();
    IntegerProperty sobelKernelSize = new SimpleIntegerProperty();
    DoubleProperty segmentationThreshold = new SimpleDoubleProperty();
    DoubleProperty vehicleFloorCutoffPercentageFix = new SimpleDoubleProperty();
    IntegerProperty roiBlurKernelSize = new SimpleIntegerProperty();
    BooleanProperty roiBlurEnabled = new SimpleBooleanProperty();
    IntegerProperty aspectRatioThresholdLimit = new SimpleIntegerProperty();
    IntegerProperty gapCloseSmallKernel = new SimpleIntegerProperty();
    IntegerProperty gapCloseBigKernel = new SimpleIntegerProperty();
    IntegerProperty morphFillKernelWidth = new SimpleIntegerProperty();
    IntegerProperty morphFillKernelHeight = new SimpleIntegerProperty();

    @FXML
    public void initialize() {
        Window.vbImages = vbImages;

        // menu da esquerda
        lvInputImages.setCellFactory(listView -> renderCellWithImage());
        lvInputImages.setItems(inputImageFiles);
        lvInputImages.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        lvInputImages.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> process());
        selectedImageFile.bind(lvInputImages.getSelectionModel().selectedItemProperty());

        lblAxlesRunning.textProperty().bind(axlesRunning);
        lblLiftedAxles.textProperty().bind(liftedAxles);

        btnProcess.disableProperty().bind(lvInputImages.getSelectionModel().selectedItemProperty().isNull());

        // menu da direita
        imageHeight.bind(spinnerImageHeight.valueProperty());
        spinnerImageHeight.setValueFactory(new SpinnerValueFactory
                .IntegerSpinnerValueFactory(50, 10_000, 500, 50));

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

        vehicleFloorCutoffPercentageFix.bind(spinnerVehicleFloorCutoffPercentageFix.valueProperty());
        spinnerVehicleFloorCutoffPercentageFix.setValueFactory(new SpinnerValueFactory
                .DoubleSpinnerValueFactory(0.01, 0.2, 0.1, 0.01));

        roiBlurEnabled.bind(ckROIBlurEnabled.selectedProperty());
        spinnerROIBlurKernelSize.disableProperty().bind(roiBlurEnabled.not());
        roiBlurKernelSize.bind(spinnerROIBlurKernelSize.valueProperty());
        spinnerROIBlurKernelSize.setValueFactory(new SpinnerValueFactory
                .IntegerSpinnerValueFactory(3, 15, 3, 2));

        aspectRatioThresholdLimit.bind(spinnerAspectRatioThresholdLimit.valueProperty());
        spinnerAspectRatioThresholdLimit.setValueFactory(new SpinnerValueFactory
                .IntegerSpinnerValueFactory(10, 500, 200, 5));
        gapCloseSmallKernel.bind(spinnerGapCloseSmallKernel.valueProperty());
        spinnerGapCloseSmallKernel.setValueFactory(new SpinnerValueFactory
                .IntegerSpinnerValueFactory(3, 15, 3, 2));
        gapCloseBigKernel.bind(spinnerGapCloseBigKernel.valueProperty());
        spinnerGapCloseBigKernel.setValueFactory(new SpinnerValueFactory
                .IntegerSpinnerValueFactory(3, 15, 5, 2));

        morphFillKernelWidth.bind(spinnerMorphFillKernelWidth.valueProperty());
        spinnerMorphFillKernelWidth.setValueFactory(new SpinnerValueFactory
                .IntegerSpinnerValueFactory(3, 97, 15, 2));
        morphFillKernelHeight.bind(spinnerMorphFillKernelHeight.valueProperty());
        spinnerMorphFillKernelHeight.setValueFactory(new SpinnerValueFactory
                .IntegerSpinnerValueFactory(3, 97, 5, 2));

    }

    @FXML
    public void menuFileOpen() {
        ConfigService.chooseInputFolder();
        List<Path> images = readImages(ConfigService.config.getInputFolder());
        inputImageFiles.setAll(images);
        // trigger render first item
        lvInputImages.getSelectionModel().selectFirst();
    }

    @FXML
    public void menuFileQuit() {
        System.exit(0);
    }

    @FXML
    public void btnProcess() {
        process();
    }

    private void process() {
        clearUI();

        if (selectedImageFile.isNull().get()) {
            return;
        }

        System.out.println(selectedImageFile.get().toString());
        new ImageProcessor().process(selectedImageFile.get().toString(), imageHeight, axlesRunning, liftedAxles,
                blurEnabled.get(), blurKernelSize.get(),
                segmentationEnabled.get(), sobelKernelSize.get(), segmentationThreshold.get(),
                vehicleFloorCutoffPercentageFix.get(),
                roiBlurEnabled.get(), roiBlurKernelSize.get(),
                aspectRatioThresholdLimit.get(), gapCloseSmallKernel.get(), gapCloseBigKernel.get(),
                morphFillKernelWidth.get(), morphFillKernelHeight.get()
        );
    }

    private void clearUI() {
        Window.clearImages();
        axlesRunning.setValue("");
        liftedAxles.setValue("");
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
