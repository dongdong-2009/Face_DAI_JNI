package com.xin.android.facerecdemo.bean;

/**
 * Project ${PROJECT}
 * Created by danny on 2017/7/24.
 */

public class ImageData {
    private long dataPtr;
    private int width;
    private int height;
    private int numChannels;

    public ImageData() {
        this.width = 0;
        this.height = 0;
        this.numChannels = 0;
    }

    public ImageData(int width, int height, int numChannels, long dataPtr) {
        this.dataPtr = dataPtr;
        this.width = width;
        this.height = height;
        this.numChannels = numChannels;
    }

    public long getDataPtr() {
        return dataPtr;
    }

    public void setDataPtr(long dataPtr) {
        this.dataPtr = dataPtr;
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

    public int getNumChannels() {
        return numChannels;
    }

    public void setNumChannels(int numChannels) {
        this.numChannels = numChannels;
    }

    @Override
    public String toString() {
        return "numChannels == " + numChannels + " width == " + width + " height == " + height +
                " dataPtr == " + dataPtr;
    }
}
