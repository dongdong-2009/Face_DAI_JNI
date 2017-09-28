package com.xin.android.facerecdemo.bean;

/**
 * Project ${PROJECT}
 * Created by danny on 2017/7/24.
 */

public class FaceInfo {
    private Rect bbox;
    private double roll;
    private double pitch;
    private double yaw;
    private double score; /**< Larger score should mean higher confidence. */

    public Rect getBbox() {
        return bbox;
    }

    public void setBbox(Rect bbox) {
        this.bbox = bbox;
    }

    public double getRoll() {
        return roll;
    }

    public void setRoll(double roll) {
        this.roll = roll;
    }

    public double getPitch() {
        return pitch;
    }

    public void setPitch(double pitch) {
        this.pitch = pitch;
    }

    public double getYaw() {
        return yaw;
    }

    public void setYaw(double yaw) {
        this.yaw = yaw;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
