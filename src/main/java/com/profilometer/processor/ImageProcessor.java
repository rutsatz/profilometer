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
        // ### Original ###
        Mat src = Imgcodecs.imread(imageFile);
        if (src.empty()) {
            System.err.println("Cannot read image: " + imageFile);
            System.exit(0);
        }
        Window.addImage(src, "Original Image", heightProperty);


        // Extrair o chão
        try {
            System.out.println("src.cols() " + src.cols() + " src.rows() " + src.rows());
            int threshold = 10; // limiar para identificar fim do chão
            double[] lastPixel = src.get(src.rows() - 1, 0); // inicia comparando o primeiro pixel
            int groundEnd = 0;
            // find where the ground ends
            for (int i = src.rows() - 2; i > 0; i--) { // percorre primeira coluna de baixo para cima
                System.out.print("i " + i + ", ");
                double[] pixel = src.get(i, 0); // BGR

//                System.out.println("\tpixel " + pixel[0] + "\t " + pixel[1] + "\t " + pixel[2] + "\t " + (pixel[0] + pixel[1] + pixel[2]));

                // Se o Blue ou o Green for maior que o threshold, indica o fim do chão
                if (Math.abs(pixel[0] - lastPixel[0]) > threshold || Math.abs(pixel[1] - lastPixel[1]) > threshold) {
                    System.out.println("======> Ground ended at " + i);
                    groundEnd = i;
                    break;
                }

                lastPixel = pixel;
            }

            Rect groundRect = new Rect(0, groundEnd, src.cols(), src.rows() - groundEnd); // recorta o chão
            Mat groundImage = new Mat(src, groundRect);
            Window.addImage(groundImage, "Ground Removed", heightProperty);


            // Remover o chão da imagem original
            Rect rectNoGround = new Rect(0, 0, src.cols(), src.rows() - groundRect.height); // remover o chão
            Mat imageNoGround = new Mat(src, rectNoGround);
            Window.addImage(imageNoGround, "Image No Ground", heightProperty);




            // ### Gray ###
            Mat srcGray = new Mat();
            Imgproc.cvtColor(imageNoGround, srcGray, Imgproc.COLOR_BGR2GRAY);
            Window.addImage(srcGray, "Gray Image", heightProperty);


            // ### Identificar fundo do veículo ###

            for (int i = src.rows() - 2; i > 0; i--) { // percorre primeira coluna de baixo para cima



            }

        } catch (Exception e) {
            e.printStackTrace();
        }


//
//        // ### Crop ###
//        Rect roi = new Rect(0, srcGray.rows() / 2, srcGray.cols(), srcGray.rows() / 2);
//        Mat croppedImage = new Mat(srcGray, roi);
//        Window.addImage(croppedImage, "ROI Image", heightProperty);
//
//
//        // ### Blur ###
//        Mat blurSrc = new Mat();
//        if (blur) {
//            Size blurKernelSize = new Size(blurKernel, blurKernel);
//            Imgproc.blur(croppedImage, blurSrc, blurKernelSize);
//            Window.addImage(blurSrc, "Blur", heightProperty);
//        } else {
//            croppedImage.copyTo(blurSrc);
//        }
//
//
//        // ### Segmentation (Canny) ###
//        // https://opencv-java-tutorials.readthedocs.io/en/latest/07-image-segmentation.html#canny-edge-detector
//        Mat cannySrc = new Mat();
//        if (segmentation) {
//            double minThreshold = segmentationMinThreshold;
//            double maxThreshold = minThreshold * 3; // Canny's recommendation
//            int sobelKernelSize = sobelKernel;
//            boolean useL2gradient = false;
//            Imgproc.Canny(blurSrc, cannySrc, minThreshold, maxThreshold, sobelKernelSize, useL2gradient);
//            Window.addImage(cannySrc, "Segmentation", heightProperty);
//        } else {
//            blurSrc.copyTo(cannySrc);
//        }
//
//
//        // ### Contours ###
//        // https://opencv-java-tutorials.readthedocs.io/en/latest/08-object-detection.html
//        Mat srcContours = new Mat(src.size(), 0);
//        Core.add(srcContours, Scalar.all(0), srcContours);
//
//        // init
//        List<MatOfPoint> contours = new ArrayList<>();
//        Mat hierarchy = new Mat();
//
//        // find contours
//        Imgproc.findContours(cannySrc, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
//
//        for (MatOfPoint point : contours) {
////            System.out.println(point);
//        }
//
//        // if any contour exist...
//        if (hierarchy.size().height > 0 && hierarchy.size().width > 0) {
//            // for each contour, display it in blue
//            for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {
//                Imgproc.drawContours(srcContours, contours, idx, new Scalar(250, 0, 0));
//            }
//            Window.addImage(srcContours, "Contours", heightProperty);
//        }


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
