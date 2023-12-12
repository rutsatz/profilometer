package com.profilometer.processor;

import com.profilometer.ui.Window;
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


    public void process(String imageFile) {
        Mat src = Imgcodecs.imread(imageFile);
        if (src.empty()) {
            System.err.println("Cannot read image: " + imageFile);
            System.exit(0);
        }
        Window.addImage(src, "Original Image");

        // ### Gray ###
        Mat srcGray = new Mat();
        Imgproc.cvtColor(src, srcGray, Imgproc.COLOR_BGR2GRAY);
        Window.addImage(srcGray, "Gray Image");

        // ### Blur ###
        Mat blurSrc = new Mat();
        Size kernelSize = new Size(3, 3);
        Imgproc.blur(srcGray, blurSrc, kernelSize);
        Window.addImage(blurSrc, "Blur");

        // ### Segmentation (Canny) ###
        // https://opencv-java-tutorials.readthedocs.io/en/latest/07-image-segmentation.html#canny-edge-detector
        Mat cannySrc = new Mat();
        double minThreshold = 3.0;
        double maxThreshold = minThreshold * 3; // Canny's recommendation
        int sobelKernelSize = 3;
        boolean useL2gradient = false;
        Imgproc.Canny(blurSrc, cannySrc, minThreshold, maxThreshold, sobelKernelSize, useL2gradient);
        Window.addImage(cannySrc, "Segmentation");


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
            Window.addImage(srcContours, "Contours");
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
