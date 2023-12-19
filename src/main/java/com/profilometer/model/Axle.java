package com.profilometer.model;

import org.opencv.core.Rect;
import org.opencv.core.Scalar;

public class Axle {

    private Rect boundingBox;
    private Scalar color;
    private boolean lifted;

    public Axle(Rect boundingBox, Scalar color, boolean lifted) {
        this.boundingBox = boundingBox;
        this.color = color;
        this.lifted = lifted;
    }

    public Rect getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(Rect boundingBox) {
        this.boundingBox = boundingBox;
    }

    public Scalar getColor() {
        return color;
    }

    public void setColor(Scalar color) {
        this.color = color;
    }

    public boolean isLifted() {
        return lifted;
    }

    public void setLifted(boolean lifted) {
        this.lifted = lifted;
    }
}
