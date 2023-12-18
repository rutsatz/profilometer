package com.profilometer.processor;

import com.profilometer.ui.Window;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class ImageProcessor {

    static {
        OpenCV.loadLocally();
    }

    public void process(String imageFile, IntegerProperty heightProperty, StringProperty axlesRunning, StringProperty liftedAxles, boolean applyBlur, int blurKernel,
                        boolean applySegmentation, int sobelKernel, double segmentationMinThreshold) {

        // ### Original Image ###
        Mat src = Imgcodecs.imread(imageFile);
        if (src.empty()) {
            System.err.println("Cannot read image: " + imageFile);
            System.exit(0);
        }
        Window.addImage(src, "Original Image", heightProperty);


        // ### extract the floor ###
        try {
            int threshold = 10; // limiar para identificar fim do chão
            double[] lastPixel = src.get(src.rows() - 1, 0); // inicia comparando o primeiro pixel
            int floorEnd = 0;

            for (int i = src.rows() - 2; i > 0; i--) { // percorre primeira coluna de baixo para cima
                double[] pixel = src.get(i, 0); // BGR

                // Se o Blue ou o Green for maior que o threshold, indica o fim do chão
                if (Math.abs(pixel[0] - lastPixel[0]) > threshold || Math.abs(pixel[1] - lastPixel[1]) > threshold) {
                    floorEnd = i;
                    break;
                }

                lastPixel = pixel;
            }

            // Display removed floor
            Rect floorRect = new Rect(0, floorEnd, src.cols(), src.rows() - floorEnd); // recorta o chão
            Mat floorImage = new Mat(src, floorRect);
            Window.addImage(floorImage, "Floor Removed", heightProperty);

            // Remover o chão da imagem original
            Rect rectNoFloor = new Rect(0, 0, src.cols(), src.rows() - floorRect.height); // remover o chão
            Mat imageNoFloor = new Mat(src, rectNoFloor);
            Window.addImage(imageNoFloor, "Image No Floor", heightProperty);


            // ### Convert to Gray ###
            Mat grayImage = new Mat();
            Imgproc.cvtColor(imageNoFloor, grayImage, Imgproc.COLOR_BGR2GRAY);
            Window.addImage(grayImage, "Gray Image", heightProperty);


            // ### Blur ###
            Mat imageWithBlur = new Mat();
            if (applyBlur) {
                Size blurKernelSize = new Size(blurKernel, blurKernel);
                Imgproc.blur(grayImage, imageWithBlur, blurKernelSize);
                Window.addImage(imageWithBlur, "Blur", heightProperty);
            } else {
                grayImage.copyTo(imageWithBlur);
            }


            // ### Identify the vehicle's floor ###
            int thresholdVehicle = 75; // limiar para identificar inicio do chão do veículo (Se for maior que threshold, é chão)
            double[] vehicleFloor = new double[imageWithBlur.cols()];
            // find where the vehicle floor starts (i = column, j = row)
            for (int i = 0; i <= imageWithBlur.cols() - 1; i++) { // percorre todas as colunas
                for (int j = imageWithBlur.rows() - 2; j > 0; j--) { // percorre todas as linhas de baixo para cima
                    double[] pixel = imageWithBlur.get(j, i); // Gray

                    // Se a escala de cinza for menor que threshold, indica inicio do objeto
                    if (pixel[0] < thresholdVehicle) {
                        vehicleFloor[i] = j;
                        break;
                    }
                }
            }

            double vehicleFloorMean = 0;
            Mat vehicleFloorEdgesImage = new Mat();
            imageWithBlur.copyTo(vehicleFloorEdgesImage);

            // Draw vehicle floor
            for (int x = 0; x < vehicleFloor.length; x++) {
                vehicleFloorMean += vehicleFloor[x];
                Imgproc.line(vehicleFloorEdgesImage, new Point(x, vehicleFloor[x]), new Point(x, vehicleFloor[x]), new Scalar(250), 1);
            }

            // Calcula a média simples de todos os pontos, para identificar onde cortar a imagem.
            // Daria para tentar fazer alguma espécia de normalização para melhorar a detecção de motos.
            int vehicleFloorCutPoint = (int) vehicleFloorMean / vehicleFloor.length;
            // Como a média geralmente pega um pouco do chassi, move o ponto de corte um pouco para baixo, para pegar somente as rodas
            vehicleFloorCutPoint = (int) (vehicleFloorCutPoint * 1.10);


            // Draw vehicle floor cut line
            Imgproc.line(vehicleFloorEdgesImage, new Point(0, vehicleFloorCutPoint), new Point(vehicleFloor.length, vehicleFloorCutPoint), new Scalar(250), 1);
            Window.addImage(vehicleFloorEdgesImage, "Vehicle Floor Edges", heightProperty);


            // ### Crop Vehicle Axles ROI ###
            Rect vehicleRect = new Rect(0, 0, vehicleFloorEdgesImage.cols(), vehicleFloorCutPoint); // recorta o chão
            Mat imageVehicleNoAxles = new Mat(imageWithBlur, vehicleRect);
            Window.addImage(imageVehicleNoAxles, "Croped Vehicle", heightProperty);


            // Get the ROI with the axles
            Rect vehicleAxlesROIRect = new Rect(0, vehicleRect.height, imageWithBlur.cols(), imageWithBlur.rows() - vehicleRect.height); // remover o chão
            Mat vehicleAxlesImage = new Mat(imageWithBlur, vehicleAxlesROIRect);
            Window.addImage(vehicleAxlesImage, "Vehicle Axles ROI", heightProperty);


            Mat vehicleAxlesImageNewBlur = new Mat();
            // Novo blur pra suavizar as bordas dos brancos trocados
            Size blurKernelSize = new Size(blurKernel, blurKernel);
            Imgproc.blur(vehicleAxlesImage, vehicleAxlesImageNewBlur, blurKernelSize);
            Window.addImage(vehicleAxlesImageNewBlur, "Blur ROI", heightProperty);


            // ### Segmentation (Canny) ###
            // https://opencv-java-tutorials.readthedocs.io/en/latest/07-image-segmentation.html#canny-edge-detector
            Mat segmentedImage = new Mat();
            if (applySegmentation) {
                double minThreshold = segmentationMinThreshold;
                double maxThreshold = minThreshold * 3; // Canny's recommendation
                int sobelKernelSize = sobelKernel;
                boolean useL2gradient = false;
                Imgproc.Canny(vehicleAxlesImageNewBlur, segmentedImage, minThreshold, maxThreshold, sobelKernelSize, useL2gradient);
                Window.addImage(segmentedImage, "Segmentation", heightProperty);
            } else {
                imageVehicleNoAxles.copyTo(segmentedImage);
            }


            int aspectRatio = segmentedImage.width() / segmentedImage.height();

            Mat gapClosedImage = new Mat();
            // Se for uma imagem muito desproporcional (muito mais larga do que alta), um kernel menor funciona melhor
            if (aspectRatio >= 200) {
                // ### Small Gap Close ###
                Mat smallGapClosedImage = new Mat();
                Size smallGapKernel = new Size(3, 3);
                Mat smallMorphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, smallGapKernel);
                Imgproc.morphologyEx(segmentedImage, smallGapClosedImage, Imgproc.MORPH_CLOSE, smallMorphKernel, new Point(-1, -1), 1, Core.BORDER_REFLECT101);
                Window.addImage(smallGapClosedImage, "Gap Close (Small Kernel)", heightProperty);
                smallGapClosedImage.copyTo(gapClosedImage);
            } else {
                // ### Big Gap Close ###
                Size gapKernel = new Size(5, 5);
                Mat morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, gapKernel);
                Imgproc.morphologyEx(segmentedImage, gapClosedImage, Imgproc.MORPH_CLOSE, morphKernel, new Point(-1, -1), 2, Core.BORDER_REFLECT101);
                Window.addImage(gapClosedImage, "Gap Close (Big Kernel)", heightProperty);
            }


            // ### Contours ###
            Mat contoursImage = new Mat(gapClosedImage.size(), CvType.CV_8UC1, Scalar.all(0));


            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();


            // Draw blank line in the top to make fill countours work
            int translateXPixels = 1;
            // Clona a imagem, porém adiciona uma linha a mais para add a linha branca no topo
            Mat imageGapClosedPlusTopBlankLine = new Mat(new Size(gapClosedImage.size().width, gapClosedImage.size().height + translateXPixels), CvType.CV_8UC1, Scalar.all(0));


            /*
            Create the 2x3 translation matrix.
                [1, 0, shiftX],
                [0, 1, shiftY]
            In our case, we are moving only 1 pixel down. So, the translation matrix is defined by:
                [1, 0, 0],
                [0, 1, 1]
             */
            Mat translate1down = new Mat(2, 3, CvType.CV_32F);
            translate1down.put(0, 0, 1);
            translate1down.put(0, 1, 0);
            translate1down.put(0, 2, 0);
            translate1down.put(1, 0, 0);
            translate1down.put(1, 1, 1);
            translate1down.put(1, 2, translateXPixels); // Move 1 pixel para baixo


            // shift image 1 pixel down
            Imgproc.warpAffine(gapClosedImage, imageGapClosedPlusTopBlankLine, translate1down, new Size(imageGapClosedPlusTopBlankLine.width(), imageGapClosedPlusTopBlankLine.height()));

            // Draw a blank line in the top of the image, so when we run the Fill contours method later, it will work properly.
            Imgproc.line(imageGapClosedPlusTopBlankLine, new Point(0, 0), new Point(imageGapClosedPlusTopBlankLine.width(), 0), Scalar.all(250), 1);


            Window.addImage(imageGapClosedPlusTopBlankLine, "Gap Closed + Top Blank Line", heightProperty);


            // CHAIN_APPROX_SIMPLE does not work for this use case
            Imgproc.findContours(imageGapClosedPlusTopBlankLine, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);


            // if any contour exist
            if (hierarchy.size().height > 0 && hierarchy.size().width > 0) {
                for (int i = 0; i < contours.size(); i++) {
                    Scalar color = new Scalar(250); // White color (you can choose your own color)

                    // Draw and fill the contour on the blank image
                    Imgproc.drawContours(contoursImage, contours, i, color, Core.FILLED, Imgproc.LINE_8, hierarchy, 50, new Point());
                }

                Window.addImage(contoursImage, "Contours", heightProperty);
            }


            // fill contours
//            Size kernelSize = new Size(15, 5);
            Size kernelSize = new Size(15, 5);
            Mat morphKernel2 = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, kernelSize);

            Mat filledImage = new Mat(segmentedImage.size(), CvType.CV_8UC1, Scalar.all(0));

            Imgproc.morphologyEx(contoursImage, filledImage, Imgproc.MORPH_OPEN, morphKernel2, new Point(-1, -1), 2);
            Window.addImage(filledImage, "Filled Contours", heightProperty);


            // ### New Contours ###
            Mat newContoursImage = new Mat(filledImage.size(), CvType.CV_8UC1, Scalar.all(0));

            List<MatOfPoint> newContours = new ArrayList<>();
            Mat newHierarchy = new Mat();
            Imgproc.findContours(filledImage, newContours, newHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

            Mat newContoursTestPointsImage = new Mat(filledImage.size(), CvType.CV_8UC1, Scalar.all(0)); // plot all contours points


            int newLiftedAxlesCount = 0;
            int newRunningAxlesCount = 0;

            // ### Count Axles ###
            for (int count = 0; count < newContours.size(); count++) {
                MatOfPoint contour = newContours.get(count);

                // Draw only the current contour to check
                for (int i = 0; i < contour.size().width; i++) {
                    for (int j = 0; j < contour.size().height; j++) {
                        double[] point = contour.get(j, i);

                        // Testar imprimir os pontos
                        Imgproc.line(newContoursTestPointsImage, new Point(point[0], point[1]), new Point(point[0], point[1]), Scalar.all(250), 1);
                    }
                }


                double maxAxleHeight = 0;
                double minX = Double.MAX_VALUE;
                double maxX = 0;

                Point[] points = contour.toArray();
                for (int i = 0; i < points.length; i++) {
                    Point point = points[i];
                    if (point.y > maxAxleHeight) {
                        maxAxleHeight = point.y;
                    }
                    if (point.x < minX) {
                        minX = point.x;
                    }
                    if (point.x > maxX) {
                        maxX = point.x;
                    }
                }

                int minAxleWidthThreshold = 50;


                // for larger images, we will have larger axles. So we need to adjust the threshold
                double calculatedMinAxleThreshold = filledImage.width() * 0.10;
                // avoid having a very large threshold for very wide images
                if (calculatedMinAxleThreshold > minAxleWidthThreshold) {
                    calculatedMinAxleThreshold = minAxleWidthThreshold;
                }


                // check for noise
                double realContourWidth = maxX - minX;


                // subtract one, because when comparing height, they need to colide to identify running axles
                int roiHeight = filledImage.height() - 1;
                if (realContourWidth <= calculatedMinAxleThreshold) {
                    continue; // discard noise
                }

                // Testa se o contorno está tocando o chão para diferenciar eixos levantados de eixos rodando.
                if (roiHeight - maxAxleHeight > 1) {
                    // eixo levantado
                    newLiftedAxlesCount++;
                } else {
                    // eixo rodando
                    newRunningAxlesCount++;
                }
            }

            System.out.println("count: lifted " + newLiftedAxlesCount + " running " + newRunningAxlesCount);

            Window.addImage(newContoursTestPointsImage, "New* Test Countourns Points", heightProperty);

            Imgproc.drawContours(newContoursImage, newContours, -1, Scalar.all(250), Core.FILLED, Imgproc.LINE_8, newHierarchy, 50, new Point());
            Window.addImage(newContoursImage, "New Contours", heightProperty);


            int liftedAxlesCount = newLiftedAxlesCount;
            int runningAxlesCount = newRunningAxlesCount;

            // update UI
            Platform.runLater(() -> {
                axlesRunning.setValue(String.valueOf(runningAxlesCount));
                liftedAxles.setValue(String.valueOf(liftedAxlesCount));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
