package com.profilometer.controller;

import com.profilometer.processor.ImageProcessor;
import com.profilometer.service.ConfigService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
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

    public static Image mat2Image(Mat frame) {
        try {
            return SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
        } catch (Exception e) {
            System.err.println("Cannot convert the Mat obejct: " + e);
            return null;
        }
    }

    private static BufferedImage matToBufferedImage(Mat original) {
        // init
        BufferedImage image = null;
        int width = original.width(), height = original.height(), channels = original.channels();
        byte[] sourcePixels = new byte[width * height * channels];
        original.get(0, 0, sourcePixels);

        if (original.channels() > 1) {
            image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        } else {
            image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        }
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);

        return image;
    }

    @FXML
    public void initialize() {
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
        vbImages.getChildren().clear();

//        inputImageFiles.remove(0,3);
        System.out.println(selectedImageFile.get().toString());
        Mat src = new ImageProcessor().process(selectedImageFile.get().toString());

        HighGui.toBufferedImage(src);


        Image image = mat2Image(src);
//        imageView.setImage(image);
        ImageView imageView = new ImageView(image);

        vbImages.getChildren().add(new Label("Original Image"));
        vbImages.getChildren().add(imageView);
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
                    imageView.setImage(new Image(path.toUri().toString(), 80, 160, true, true, true));
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
