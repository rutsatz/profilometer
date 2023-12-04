package com.profilometer.processor;

import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class ImageProcessor {

    static {
        OpenCV.loadLocally();
    }

    int DELAY_CAPTION = 1500;
    int DELAY_BLUR = 100;
    int MAX_KERNEL_LENGTH = 31;
    Mat src = new Mat(), dst = new Mat();
    String windowName = "Filter Demo 1";

    private Mat srcGray = new Mat();

    public Mat process(String imageFile) {
        Mat src = Imgcodecs.imread(imageFile);
        if (src.empty()) {
            System.err.println("Cannot read image: " + imageFile);
            System.exit(0);
        }
        Imgproc.cvtColor(src, srcGray, Imgproc.COLOR_BGR2GRAY);
//        dst = srcGray;
//        displayDst(DELAY_CAPTION, srcGray);
        return src;
    }

    int displayCaption(String caption) {
        dst = Mat.zeros(src.size(), src.type());
        Imgproc.putText(dst, caption,
                new Point(src.cols() / 4, src.rows() / 2),
                Imgproc.FONT_HERSHEY_COMPLEX, 1, new Scalar(255, 255, 255));
        return displayDst(DELAY_CAPTION, dst);
    }
    int displayDst(int delay, Mat dst) {
        HighGui.imshow( windowName, dst );
        int c = HighGui.waitKey( delay );
        if (c >= 0) { return -1; }
        return 0;
    }

}
