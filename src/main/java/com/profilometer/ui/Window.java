package com.profilometer.ui;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class Window {

    public static VBox vbImages;

    public static void addImage(Mat mat, String name, IntegerProperty heightProperty) {
        HighGui.toBufferedImage(mat);

        Image image = mat2Image(mat);

        Label title = new Label(name);
        VBox.setMargin(title, new Insets(20, 5, 0, 5));

        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.fitWidthProperty().bind(heightProperty);

        updateUi(() -> {
            vbImages.getChildren().add(title);
            vbImages.getChildren().add(imageView);
        });
    }

    private static void updateUi(Runnable runnable) {
        Platform.runLater(runnable);
    }

    public static void clearImages() {
        vbImages.getChildren().clear();
    }

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

}
