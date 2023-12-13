package com.profilometer.processor;

import com.profilometer.ui.Window;
import javafx.beans.property.IntegerProperty;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class ImageProcessor {

    static {
        OpenCV.loadLocally();
    }

    int DELAY_CAPTION = 1500;
    int DELAY_BLUR = 100;
    int MAX_KERNEL_LENGTH = 31;

    String windowName = "Filter Demo 1";


    public void process(String imageFile, IntegerProperty heightProperty, boolean blur, int blurKernel,
                        boolean segmentation, int sobelKernel, double segmentationMinThreshold) {
        Mat src = Imgcodecs.imread(imageFile);
        if (src.empty()) {
            System.err.println("Cannot read image: " + imageFile);
            System.exit(0);
        }
        Window.addImage(src, "Original Image", heightProperty);

        // ### Gray ###
        Mat srcGray = new Mat();
        Imgproc.cvtColor(src, srcGray, Imgproc.COLOR_BGR2GRAY);
        Window.addImage(srcGray, "Gray Image", heightProperty);

        // ### Blur ###
        Mat blurSrc = new Mat();
        if (blur) {
            Size blurKernelSize = new Size(blurKernel, blurKernel);
            Imgproc.blur(srcGray, blurSrc, blurKernelSize);
            Window.addImage(blurSrc, "Blur", heightProperty);
        } else {
            srcGray.copyTo(blurSrc);
        }

        // ### Segmentation (Canny) ###
        // https://opencv-java-tutorials.readthedocs.io/en/latest/07-image-segmentation.html#canny-edge-detector
        Mat cannySrc = new Mat();
        if (segmentation) {
            double minThreshold = segmentationMinThreshold;
            double maxThreshold = minThreshold * 3; // Canny's recommendation
            int sobelKernelSize = sobelKernel;
            boolean useL2gradient = false;
            Imgproc.Canny(blurSrc, cannySrc, minThreshold, maxThreshold, sobelKernelSize, useL2gradient);
            Window.addImage(cannySrc, "Segmentation", heightProperty);
        } else {
            blurSrc.copyTo(cannySrc);
        }

        // ### Contours ###
        // https://opencv-java-tutorials.readthedocs.io/en/latest/08-object-detection.html
        Mat srcContours = new Mat(src.size(), 0);
        Core.add(srcContours, Scalar.all(0), srcContours);

        // init
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        // find contours
        Imgproc.findContours(cannySrc, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

        for (MatOfPoint point : contours) {
//            System.out.println(point);
        }

        // if any contour exist...
        if (hierarchy.size().height > 0 && hierarchy.size().width > 0) {
            // for each contour, display it in blue
            for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {
                Imgproc.drawContours(srcContours, contours, idx, new Scalar(250, 0, 0));
            }
            Window.addImage(srcContours, "Contours", heightProperty);
        }


    }

    int displayCaption(Mat src, Mat dst, String caption) {
        dst = Mat.zeros(src.size(), src.type());
        Imgproc.putText(dst, caption,
                new Point(src.cols() / 4, src.rows() / 2),
                Imgproc.FONT_HERSHEY_COMPLEX, 1, new Scalar(255, 255, 255));
        return displayDst(DELAY_CAPTION, dst);
    }

    int displayDst(int delay, Mat dst) {
        HighGui.imshow(windowName, dst);
        int c = HighGui.waitKey(delay);
        if (c >= 0) {
            return -1;
        }
        return 0;
    }

}
