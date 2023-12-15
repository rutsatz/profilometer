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
    private static int MAX_BINARY_VALUE = 255;

    String windowName = "Filter Demo 1";


    public void process(String imageFile, IntegerProperty heightProperty, boolean blur, int blurKernel,
                        boolean segmentation, int sobelKernel, double segmentationMinThreshold) {

        // ### Original Image ###
        Mat src = Imgcodecs.imread(imageFile);
        if (src.empty()) {
            System.err.println("Cannot read image: " + imageFile);
            System.exit(0);
        }
        Window.addImage(src, "Original Image", heightProperty);


        // Extrair o chão
        try {
            int threshold = 10; // limiar para identificar fim do chão
            double[] lastPixel = src.get(src.rows() - 1, 0); // inicia comparando o primeiro pixel
            int groundEnd = 0;

            for (int i = src.rows() - 2; i > 0; i--) { // percorre primeira coluna de baixo para cima
                double[] pixel = src.get(i, 0); // BGR

                // Se o Blue ou o Green for maior que o threshold, indica o fim do chão
                if (Math.abs(pixel[0] - lastPixel[0]) > threshold || Math.abs(pixel[1] - lastPixel[1]) > threshold) {
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


            // ### Blur ###
            Mat blurSrc = new Mat();
            if (blur) {
                Size blurKernelSize = new Size(blurKernel, blurKernel);
                Imgproc.blur(srcGray, blurSrc, blurKernelSize);
                Window.addImage(blurSrc, "Blur", heightProperty);
            } else {
                srcGray.copyTo(blurSrc);
            }


            // ### Identificar fundo do veículo ###
            int thresholdVehicle = 75; // limiar para identificar inicio do chão do veículo (Se for maior que threshold, é fundo)
            double[] vehicleFloor = new double[blurSrc.cols()];
            // find where the vehicle ground starts (i = column, j = row)
            for (int i = 0; i <= blurSrc.cols() - 1; i++) { // percorre todas as colunas
                for (int j = blurSrc.rows() - 2; j > 0; j--) { // percorre todas as linhas de baixo para cima
                    double[] pixel = blurSrc.get(j, i); // Gray

                    // Se a escala de cinza for menor que threshold, indica inicio do objeto
                    if (pixel[0] < thresholdVehicle) {
                        vehicleFloor[i] = j;
                        break;
                    }
                }
            }

            double vehicleFloorMean = 0;
            Mat vehicleFloorEdgesImage = new Mat();
            blurSrc.copyTo(vehicleFloorEdgesImage);

            // Draw vehicle floor
            for (int x = 0; x < vehicleFloor.length; x++) {
                vehicleFloorMean += vehicleFloor[x];
                Imgproc.line(vehicleFloorEdgesImage, new Point(x, vehicleFloor[x]), new Point(x, vehicleFloor[x]), new Scalar(255), 1);
            }

            // Calcula a média simples de todos os pontos, para identificar onde cortar a imagem.
            // Daria para tentar fazer alguma espécia de normalização para melhorar a detecção de motos.
            int vehicleFloorCutPoint = (int) vehicleFloorMean / vehicleFloor.length;
            // Como a média geralmente pega um pouco do chassi, move o ponto de corte um pouco para baixo, para pegar somente as rodas
            vehicleFloorCutPoint = (int) (vehicleFloorCutPoint * 1.10);

            // Draw vehicle floor cut line
            Imgproc.line(vehicleFloorEdgesImage, new Point(0, vehicleFloorCutPoint), new Point(vehicleFloor.length, vehicleFloorCutPoint), new Scalar(255), 1);
            Window.addImage(vehicleFloorEdgesImage, "Vehicle Floor Edges", heightProperty);


            // ### Crop Vehicle Axles ROI ###
            Rect vehicleRect = new Rect(0, 0, vehicleFloorEdgesImage.cols(), vehicleFloorCutPoint); // recorta o chão
            Mat imageVehicleNoAxles = new Mat(blurSrc, vehicleRect);
            Window.addImage(imageVehicleNoAxles, "Croped Vehicle", heightProperty);

            // Get the ROI with the axles
            Rect vehicleAxlesROIRect = new Rect(0, vehicleRect.height, blurSrc.cols(), blurSrc.rows() - vehicleRect.height); // remover o chão
            Mat vehicleAxlesImage = new Mat(blurSrc, vehicleAxlesROIRect);
            Window.addImage(vehicleAxlesImage, "Vehicle Axles ROI", heightProperty);


            // TODO Transformar as cores brancas em preto.
//            Mat whiteRemovedImage = new Mat();
//            vehicleAxlesImage.copyTo(whiteRemovedImage);
//            int upperWhiteThreshold = 150;
//            double upperBlackTarget = 0;
//            int mediumWhiteThreshold = 150;
//            double mediumBlackTarget = 50;
//            for (int i = 0; i < whiteRemovedImage.cols(); i++) { // width
//                for (int j = 0; j < whiteRemovedImage.rows(); j++) { // height
//                    double[] pixel = whiteRemovedImage.get(j, i);
//                    if (pixel[0] > upperWhiteThreshold) { // Set super white to super black
//                        whiteRemovedImage.at(Double.class, j, i).setV(upperBlackTarget*0.75);
//                    }
////                    else if (pixel[0] > mediumWhiteThreshold) { // Reduce the whiteness of adjacent pixels
////                        whiteRemovedImage.at(Double.class, j, i).setV(mediumBlackTarget);
////                    }
//                }
//            }
//            Window.addImage(whiteRemovedImage, "White Removed", heightProperty);



//            Mat dest = new Mat();
//            // Novo blur pra suavizar as bordas dos brancos trocados
//            Size blurKernelSize = new Size(blurKernel, blurKernel);
//            Imgproc.blur(whiteRemovedImage, dest, blurKernelSize);
//            Window.addImage(dest, "Blur White Removed", heightProperty);

            // ### Segmentation (Canny) ###
            // https://opencv-java-tutorials.readthedocs.io/en/latest/07-image-segmentation.html#canny-edge-detector
            Mat segmentedImage = new Mat();
            if (segmentation) {
                double minThreshold = segmentationMinThreshold;
                double maxThreshold = minThreshold * 3; // Canny's recommendation
                int sobelKernelSize = sobelKernel;
                boolean useL2gradient = false;
                Imgproc.Canny(vehicleAxlesImage, segmentedImage, minThreshold, maxThreshold, sobelKernelSize, useL2gradient);
                Window.addImage(segmentedImage, "Segmentation", heightProperty);
            } else {
                imageVehicleNoAxles.copyTo(segmentedImage);
            }


            // Gap Close
            Mat gapClosedImage = new Mat();
            Size gapKernel = new Size(5, 5);
            Mat morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, gapKernel);
            Imgproc.morphologyEx(segmentedImage, gapClosedImage, Imgproc.MORPH_CLOSE, morphKernel, new Point(-1,-1), 2, Core.BORDER_REFLECT101);
            Window.addImage(gapClosedImage, "Gap Close", heightProperty);



//            // Count Axles
//            int axleSizeThreshold = 20;
//            int axleWhiteColorThreshold = 150;
//            boolean
//            for (int i = 0; i < gapClosedImage.cols() ; i++) { // percorre todas as colunas
////                for (int j = gapClosedImage.rows(); j > 0; j--) { // percorre todas as linhas de baixo para cima
//                    double[] pixel = gapClosedImage.get(0, i); // Gray
//
//                    // Se a escala de cinza for menor que threshold, indica inicio do objeto
//                    if (pixel[0] < axleWhiteColorThreshold) {
//                        vehicleFloor[i] = j;
//                        break;
//                    }
////                }
//            }




            // ### Contours ###
            // https://opencv-java-tutorials.readthedocs.io/en/latest/08-object-detection.html
//            Mat contoursImage = new Mat(gapClosedImage.size(), 0);
//            Core.add(contoursImage, Scalar.all(0), contoursImage);
//
//            // init
//            List<MatOfPoint> contours = new ArrayList<>();
//            Mat hierarchy = new Mat();
//
//            // find contours
//            Imgproc.findContours(gapClosedImage, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
//
//            for (MatOfPoint point : contours) {
//                System.out.println(point);
//            }
//
//            // if any contour exist...
//            if (hierarchy.size().height > 0 && hierarchy.size().width > 0) {
//                // for each contour, display it in blue
//                for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {
//                    Imgproc.drawContours(contoursImage, contours, idx, new Scalar(250, 0, 0));
//                }
//                Window.addImage(contoursImage, "Contours", heightProperty);
//            }

        } catch (Exception e) {
            e.printStackTrace();
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
