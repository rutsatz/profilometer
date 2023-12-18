package com.profilometer.processor;

import com.profilometer.ui.Window;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImageProcessor {

    private static final int MAX_BINARY_VALUE = 255;

    static {
        OpenCV.loadLocally();
    }

    int DELAY_CAPTION = 1500;
    String windowName = "Filter Demo 1";


    public void process(String imageFile, IntegerProperty heightProperty, StringProperty axlesRunning, StringProperty liftedAxles, boolean blur, int blurKernel,
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
//            Window.addImage(groundImage, "Ground Removed", heightProperty); // TODO uncomment

            // Remover o chão da imagem original
            Rect rectNoGround = new Rect(0, 0, src.cols(), src.rows() - groundRect.height); // remover o chão
            Mat imageNoGround = new Mat(src, rectNoGround);
//            Window.addImage(imageNoGround, "Image No Ground", heightProperty); // TODO uncomment


            // ### Gray ###
            Mat srcGray = new Mat();
            Imgproc.cvtColor(imageNoGround, srcGray, Imgproc.COLOR_BGR2GRAY);
//            Window.addImage(srcGray, "Gray Image", heightProperty); // TODO uncomment


            // ### Blur ###
            Mat blurSrc = new Mat();
            if (blur) {
                Size blurKernelSize = new Size(blurKernel, blurKernel);
                Imgproc.blur(srcGray, blurSrc, blurKernelSize);
//                Window.addImage(blurSrc, "Blur", heightProperty); // TODO uncomment
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
            Mat imageVehicleNoAxles = new Mat(blurSrc, vehicleRect);
//            Window.addImage(imageVehicleNoAxles, "Croped Vehicle", heightProperty); // TODO uncomment


            // Get the ROI with the axles
            Rect vehicleAxlesROIRect = new Rect(0, vehicleRect.height, blurSrc.cols(), blurSrc.rows() - vehicleRect.height); // remover o chão
//            System.out.println("ROI height: " + vehicleAxlesROIRect.height);
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


            Mat vehicleAxlesImageNewBlur = new Mat();
            // Novo blur pra suavizar as bordas dos brancos trocados
            Size blurKernelSize = new Size(blurKernel, blurKernel);
            Imgproc.blur(vehicleAxlesImage, vehicleAxlesImageNewBlur, blurKernelSize);
//            Window.addImage(vehicleAxlesImageNewBlur, "Blur ROI", heightProperty); // TODO uncomment



            // ### Segmentation (Canny) ###
            // https://opencv-java-tutorials.readthedocs.io/en/latest/07-image-segmentation.html#canny-edge-detector
            Mat segmentedImage = new Mat();
            if (segmentation) {
                double minThreshold = segmentationMinThreshold;
                double maxThreshold = minThreshold * 3; // Canny's recommendation
                int sobelKernelSize = sobelKernel;
                boolean useL2gradient = false;
                Imgproc.Canny(vehicleAxlesImageNewBlur, segmentedImage, minThreshold, maxThreshold, sobelKernelSize, useL2gradient);
                Window.addImage(segmentedImage, "Segmentation", heightProperty);
            } else {
                imageVehicleNoAxles.copyTo(segmentedImage);
            }



            // ### Dilated Image ### // Reforcar as bordas
//            Mat dilatedImage = new Mat();
//            Size dilatedKernelSize = new Size(3, 3);
//            Mat dilatedKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, dilatedKernelSize);
//            Imgproc.morphologyEx(segmentedImage, dilatedImage, Imgproc.MORPH_ERODE, dilatedKernel, new Point(-1, -1), 2, Core.BORDER_REFLECT101);
//            Window.addImage(dilatedImage, "Dilated Image", heightProperty);



//            // ### Remove External Lines ###
//            Mat removedExternalLinesImage = new Mat();
//            Size extKernelSize = new Size(15, 15);
//            Mat extKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, extKernelSize);
//            Imgproc.morphologyEx(segmentedImage, removedExternalLinesImage, Imgproc.MORPH_OPEN, extKernel, new Point(-1, -1), 2, Core.BORDER_REFLECT101);
//            Window.addImage(removedExternalLinesImage, "Remove External Lines", heightProperty);


            int aspectRatio = segmentedImage.width() / segmentedImage.height();
//            System.out.println("aspectRatio " + aspectRatio);

            Mat gapClosedImage = new Mat();

            // Se for uma imagem muito desproporcional (muito mais larga do que alta), um kernel menor funciona melhor
            if (aspectRatio >= 200) {
                Mat smallGapClosedImage = new Mat();
                Size smallGapKernel = new Size(3, 3);
                Mat smallMorphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, smallGapKernel);
                Imgproc.morphologyEx(segmentedImage, smallGapClosedImage, Imgproc.MORPH_CLOSE, smallMorphKernel, new Point(-1, -1), 1, Core.BORDER_REFLECT101);
                Window.addImage(smallGapClosedImage, "Gap Close (Small Kernel)", heightProperty);
                smallGapClosedImage.copyTo(gapClosedImage);
            } else {
                // ### Gap Close ###
                Size gapKernel = new Size(5, 5);
                Mat morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, gapKernel);
                Imgproc.morphologyEx(segmentedImage, gapClosedImage, Imgproc.MORPH_CLOSE, morphKernel, new Point(-1, -1), 2, Core.BORDER_REFLECT101);
                Window.addImage(gapClosedImage, "Gap Close (Big Kernel)", heightProperty);
            }




            // ### Contours ###
            Mat contoursImage = new Mat(gapClosedImage.size(), CvType.CV_8UC1, Scalar.all(0));

//            contoursImage.setTo(Scalar.all(128)); // preenche de preto

            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();

//            Core.add(gapClosedImage, Scalar.all(0), contoursImage);



            // Draw blank line in the top to make fill countours work
            int translateXPixels  = 1 ;
            Mat imageGapClosedPlusTopBlankLine = new Mat(new Size(gapClosedImage.size().width, gapClosedImage.size().height + translateXPixels), CvType.CV_8UC1, Scalar.all(0)); // Clona a imagem, porém adiciona uma linha a mais para add a linha branca no topo
//            gapClosedImage.copyTo(imageGapClosedPlusTopBlankLine);
            Mat translate1down = new Mat(2, 3, CvType.CV_32F);
            translate1down.put(0, 0, 1);
            translate1down.put(0, 1, 0);
            translate1down.put(0, 2, 0);
            translate1down.put(1, 0, 0);
            translate1down.put(1, 1, 1);
            translate1down.put(1, 2, translateXPixels); // Move 1 pixel para baixo


            // shift image 1 pixel down
            Imgproc.warpAffine(gapClosedImage, imageGapClosedPlusTopBlankLine, translate1down, new Size(imageGapClosedPlusTopBlankLine.width(), imageGapClosedPlusTopBlankLine.height()));
            Imgproc.line(imageGapClosedPlusTopBlankLine, new Point(0,0), new Point(imageGapClosedPlusTopBlankLine.width(), 0), Scalar.all(250), 1);
//            Imgproc.line(imageGapClosedPlusTopBlankLine, new Point(0,6), new Point(imageGapClosedPlusTopBlankLine.width(), 6), Scalar.all(250), 1);
            // Draw only where we have contours, to avoid combining unrelated objects. Actually, add the additional points.

//            for (int x = 0; x < imageGapClosedPlusTopBlankLine.width(); x++) {
//                double[] pixel = imageGapClosedPlusTopBlankLine.get(7,x);
//                if (pixel[0] > 128) {
//                    Imgproc.line(imageGapClosedPlusTopBlankLine, new Point(x, 7), new Point(x, 0), Scalar.all(250), 1);
//                }
//            }

            Window.addImage(imageGapClosedPlusTopBlankLine, "Gap Closed + Top Blank Line", heightProperty);




//            Mat newGapClosedImage = new Mat();
//            Size newGapKernel = new Size(3, 3);
//            Mat newMorphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, newGapKernel);
//            Imgproc.morphologyEx(imageGapClosedPlusTopBlankLine, newGapClosedImage, Imgproc.MORPH_CLOSE, newMorphKernel, new Point(-1, -1), 2, Core.BORDER_REFLECT101);
//            Window.addImage(newGapClosedImage, "New Gap Close", heightProperty);





//            Imgproc.findContours(gapClosedImage, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            Imgproc.findContours(imageGapClosedPlusTopBlankLine, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);



            // Test
            Mat contoursTestPointsImage = new Mat(imageGapClosedPlusTopBlankLine.size() ,CvType.CV_8UC1, Scalar.all(0)); // plot all contours points
            Mat minsMaxsTestPointsImage = new Mat(imageGapClosedPlusTopBlankLine.size() ,CvType.CV_8UC1, Scalar.all(0)); // plot only mins and maxs contours
//            Imgproc.(contoursTestPointsImage, Scalar.all(0), contoursTestPointsImage); // black fill




//
            for (int count = 0; count < contours.size(); count++) {
//                System.out.println("contour " + count);
//                if (count != 3) continue; // TODO: remove

                MatOfPoint contour = contours.get(count);
//                Point[] points = contour.toArray();
//                Point minX1 = new Point(Double.MAX_VALUE,0);
//                int minXListPos = 0; // Posićão que o ponto deve ser inserido na lista
//                Point maxX1 = new Point(0,0);
//                int maxXListPos = 0;
//                for (int x = 0; x < points.length; x++) {
//                    Point point = points[x];
//                    if (point.x < minX1.x) {
//                        minX1 = new Point(point.x, 0);
//                        minXListPos = x;
//                    }
//                    if (point.x > maxX1.x) {
//                        maxX1 = new Point(point.x, 0);
//                        maxXListPos = x;
//                    }
//                }
//
//
//                System.out.println("contour " + count + " area " + Imgproc.contourArea(contour));
//
//                // Create a new MatOfPoint contour with the new top line
//                System.out.println( "points.length " + points.length + " minX1 " + minX1 + " maxX1 " + maxX1 + ". minXListPos " + minXListPos + " maxXListPos " + maxXListPos);
//
//                List<Point> newPoints = new ArrayList<>(Arrays.asList(contour.toArray()));
//
//                newPoints.add(points.length, maxX1); // Adiciona o último primeiro
//                newPoints.add(0, minX1); // Adiciona o último primeiro
//
//                System.out.println("new contour " + count + " new area " + Imgproc.contourArea(contour));



//                System.out.println("--------------> contourArea: " + Imgproc.contourArea(contour));


//                System.out.print("type "  + point.type() + " width " + point.width() + " height " + point.height());
//                System.out.println();
//                System.out.print(" arr " + Arrays.toString(point.toArray()));
//                System.out.println();
//                for (Point p : point.toList()) {
//                }
//                System.out.print("contour cols: " + contour.cols() + " rows: " + contour.rows() + " points: ");


                double[] minX = {0,0};
                double[] maxX = {0,0};
                double[] minY = {0,0};
                double[] maxY = {0,0};


                // extract mins and max
                for(int i = 0; i < contour.size().width; i++){
                    for (int j=0; j < contour.size().height; j++) {
                        double[] point = contour.get(j, i);
//                        System.out.print("(" + (int) point[0] + ", " + (int) point[1] + ") ");

                        // Testar imprimir os pontos
                        Imgproc.line(contoursTestPointsImage, new Point(point[0], point[1]), new Point(point[0], point[1]), Scalar.all(250), 1);

                        if (point[0] < minX[0]) {
                            minX = point;
                        }
                        if (point[0] > maxX[0]) {
                            maxX = point;
                        }
                        if (point[1] < minY[1]) {
                            minY = point;
                        }
                        if (point[1] > maxY[1]) {
                            maxY = point;
                        }
                    }
                }

//                if ()

                Imgproc.line(minsMaxsTestPointsImage, new Point(minX), new Point(minX), Scalar.all(250), 3);
                Imgproc.line(minsMaxsTestPointsImage, new Point(maxX), new Point(maxX), Scalar.all(250), 3);
                Imgproc.line(minsMaxsTestPointsImage, new Point(minY), new Point(minY), Scalar.all(250), 3);
                Imgproc.line(minsMaxsTestPointsImage, new Point(maxY), new Point(maxY), Scalar.all(250), 3);


                System.out.println();
            }

            Window.addImage(contoursTestPointsImage, "Test Countourns Points", heightProperty);



//            Window.addImage(minsMaxsTestPointsImage, "minsMaxsTestPointsImage Test Countourns Points", heightProperty);


            // Depois de pegar os pontos, posso contar quantos tocam o chão. Se for maior que o threshold, baaaam!!!
            // Se tiver pontos dentro de um threshold, ou seja, é um pneu, mas não toca o solo, baammmm, eixo levantado!!!!!!



            // if any contour exist...
            if (hierarchy.size().height > 0 && hierarchy.size().width > 0) {
                // for each contour, display it in blue
//                for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {
//                    Imgproc.drawContours(contoursImage, contours, idx, new Scalar(250, 0, 0), Imgproc.FILLED);
//                }

//                Imgproc.drawContours(contoursImage, contours, -1, new Scalar(250, 0, 0), Imgproc.FILLED);
//                Imgproc.drawContours(contoursImage, contours, -1, new Scalar(250, 0, 0), Core.FILLED);

                for (int i = 0; i < contours.size(); i++) {
                    MatOfPoint contour = contours.get(i);
                    Scalar color = new Scalar(255, 0, 0); // White color (you can choose your own color)

                    // Draw and fill the contour on the blank image
                    Imgproc.drawContours(contoursImage, contours, i, color, Core.FILLED, Imgproc.LINE_8, hierarchy, 50, new Point());
                }

                Window.addImage(contoursImage, "Contours", heightProperty);
            }


            // fill contours
//            Size kernelSize = new Size(15, 5);
            Size kernelSize = new Size(15, 5);
            Mat morphKernel2 = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, kernelSize);
//            Mat morphKernel2 = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, kernelSize);

            Mat filledImage = new Mat(segmentedImage.size(), CvType.CV_8UC1, Scalar.all(0));
//            Core.add(filledImage, Scalar.all(0), filledImage);

//            Imgproc.morphologyEx(segmentedImage, filledImage, Imgproc.MORPH_OPEN, morphKernel2, new Point(-1,-1), 2);
            Imgproc.morphologyEx(contoursImage, filledImage, Imgproc.MORPH_OPEN, morphKernel2, new Point(-1, -1), 2);
            Window.addImage(filledImage, "Filled Contours", heightProperty);





            // ### New Contours ###
            Mat newContoursImage = new Mat(filledImage.size(), CvType.CV_8UC1, Scalar.all(0));

//            contoursImage.setTo(Scalar.all(128)); // preenche de preto

            List<MatOfPoint> newContours = new ArrayList<>();
            Mat newHierarchy = new Mat();
            Imgproc.findContours(filledImage, newContours, newHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

            Mat newContoursTestPointsImage = new Mat(filledImage.size() ,CvType.CV_8UC1, Scalar.all(0)); // plot all contours points


            int newLiftedAxlesCount = 0;
            int newRunningAxlesCount = 0;


            for (int count = 0; count < newContours.size(); count++) {
//                if (count != 2) continue; // TODO remove

                MatOfPoint contour = newContours.get(count);
                Scalar color = new Scalar(255, 0, 0); // White color (you can choose your own color)


                // Draw only the current contour to check
                for(int i = 0; i < contour.size().width; i++){
                    for (int j=0; j < contour.size().height; j++) {
                        double[] point = contour.get(j, i);
//                        System.out.print("(" + (int) point[0] + ", " + (int) point[1] + ") ");

                        // Testar imprimir os pontos
                        Imgproc.line(newContoursTestPointsImage, new Point(point[0], point[1]), new Point(point[0], point[1]), Scalar.all(250), 1);

                    }
                }

                double maxAxleHeight = 0;
                double minX = Double.MAX_VALUE;
                double maxX = 0;

//                for(int i = 0; i < contour.size().width; i++){
//                    for (int j=0; j < contour.size().height; j++) {
//                        double[] point = contour.get(j, i);
//                        System.out.print("(" + (int) point[0] + ", " + (int) point[1] + ") ");
//                        if (point[1] < minHeight) {
//                            minHeight = (int) point[1];
//                        }
//                    }
//                }


                Point[] points = contour.toArray();
                for (int i =0; i < points.length; i++) {
                    Point point = points[i];
//                    System.out.print("(" + point.x + ", " + point.y + ") ");
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

                int minAxleWidthThreashold = 50;


                // for larger images, we will have larger axles. So we need to adjust the threshold
                // 50 - 1200
                // 20 - 137
                double calculatedMinAxleThreshold = filledImage.width() * 0.10;
                // avoid having a very large threshold for very wide images
                if (calculatedMinAxleThreshold > minAxleWidthThreashold) {
                    calculatedMinAxleThreshold = minAxleWidthThreashold;
                }



                // check for noise
                double realCountourWidth = maxX - minX;


                int roiHeight = filledImage.height();
                System.out.println("axle \t" + count + " - roiHeight \t" + roiHeight + " axleHeight \t" + maxAxleHeight +
                        " diff \t" + (roiHeight - maxAxleHeight) + " minX \t" + minX +
                        " maxX \t" + maxX + " realCountourWidth \t" + realCountourWidth);
                System.out.println("image width " + filledImage.width() + " calculatedMinAxleThreshold " + calculatedMinAxleThreshold);

                if (realCountourWidth <= calculatedMinAxleThreshold) {
                    continue; // discard noise
                }

                // Testa se o contorno está tocando o chão para identificar eixos levantados de eixos rodando.
                if ( roiHeight - maxAxleHeight > 1 ) {
                    // eixo levantado
                    newLiftedAxlesCount++;
                } else {
                    // eixo rodando
                    newRunningAxlesCount++;
                }

//                System.out.println("new contour " + count + " width " + contour.width() + " height " + contour.height() + " roiHeight " + roiHeight
//                + " maxHeight " + maxHeight);
//                System.out.println("--------------> new contourArea: " + Imgproc.contourArea(contour)); //TODO uncomment

                // Draw and fill the contour on the blank image
//                Imgproc.drawContours(newContoursImage, newContours, i, color, Core.FILLED, Imgproc.LINE_8, hierarchy, 0, new Point());
            }

            System.out.println("count: lifted " + newLiftedAxlesCount + " running " + newRunningAxlesCount);

            Window.addImage(newContoursTestPointsImage, "New* Test Countourns Points", heightProperty);


            Imgproc.drawContours(newContoursImage, newContours, -1, Scalar.all(250), Core.FILLED, Imgproc.LINE_8, newHierarchy, 50, new Point());
            Window.addImage(newContoursImage, "New Contours", heightProperty);







            // ### Count Axles ###
            Mat countAxlesImageSrc = new Mat();
            // src image
            newContoursImage.copyTo(countAxlesImageSrc);

            int axleMinSizeThreshold = 15;
            int axleWhiteColorThreshold = 150;
            boolean insideObject = false;
            int objectStart = 0;
            int objectEnd = 0;
            int lastObjectEnd = 0;
            int totalAxlesCount = 0;
            int analyzeHeight = 1;

            // Handle cases where resolution is too low
            int columnHeight = countAxlesImageSrc.height() - analyzeHeight;
            if (columnHeight <= 0) {
                columnHeight = countAxlesImageSrc.height();
            }
            for (int x = 0; x < countAxlesImageSrc.width(); x++) { // percorre todas as colunas
//                System.out.println("width " + countAxlesImageSrc.width() + " height " + countAxlesImageSrc.height() );
                double[] pixel = countAxlesImageSrc.get(countAxlesImageSrc.height() - columnHeight, x); // Gray
//                System.out.print("(" + (countAxlesImageSrc.height() - columnHeight) + "," + x + ") \t");
//                System.out.println("x \t" + x + "\t " + pixel[0] + "\t " + insideObject + "\t " + objectStart + "\t " + objectEnd + "\t " + lastObjectEnd);
                // Se a escala de cinza for menor que threshold, indica inicio do objeto
                if (!insideObject && pixel[0] > axleWhiteColorThreshold) {
                    objectStart = x;
                    insideObject = true;
                }
                if (insideObject && pixel[0] < axleWhiteColorThreshold) {
                    insideObject = false;
                    objectEnd = x;
                    int objectLenght = objectEnd - objectStart;
//                    System.out.println("objectLenght " + objectLenght);
                    if (objectLenght >= axleMinSizeThreshold) {
                        totalAxlesCount++;
                    }
                    lastObjectEnd = objectEnd;
                }
            }
            System.out.println("old method totalAxlesCount " + totalAxlesCount);

            int liftedAxlesCount = newLiftedAxlesCount;
            int runningAxlesCount = newRunningAxlesCount;



            // update UI
            Platform.runLater(() -> {
                axlesRunning.setValue(String.valueOf(runningAxlesCount));
                liftedAxles.setValue(String.valueOf(liftedAxlesCount));
            });


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
