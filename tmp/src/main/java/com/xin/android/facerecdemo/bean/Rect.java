package com.xin.android.facerecdemo.bean;

import org.opencv.core.Point;

/**
 * Project ${PROJECT}
 * Created by danny on 2017/7/24.
 */

public class Rect {
    private int x;
    private int y;
    private int width;
    private int height;

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Point tl() {
        return new Point(x, y);
    }

    public Point br() {
        return new Point(x + width, y + height);
    }

    @Override
    public String toString() {
        return "x == " + x + " y == " + y + " width == " + width + " height == " + height;
    }
}
