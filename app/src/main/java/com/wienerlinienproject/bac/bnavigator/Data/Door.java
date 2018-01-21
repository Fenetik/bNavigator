package com.wienerlinienproject.bac.bnavigator.Data;


public class Door {

    private String position; //left, bottom, top, right
    private double startPointX;
    private double startPointY;
    private double endPointX;
    private double endPointY;

    public Door(String position, double startPointX, double startPointY, double endPointX, double endPointY){
        this.position = position;
        this.startPointX = startPointX;
        this.startPointY = startPointY;
        this.endPointX = endPointX;
        this.endPointY = endPointY;
    }

    public double getStartPointX() {
        return startPointX;
    }

    public void setStartPointX(double startPointX) {
        this.startPointX = startPointX;
    }

    public double getStartPointY() {
        return startPointY;
    }

    public void setStartPointY(double startPointY) {
        this.startPointY = startPointY;
    }

    public double getEndPointX() {
        return endPointX;
    }

    public void setEndPointX(double endPointX) {
        this.endPointX = endPointX;
    }

    public double getEndPointY() {
        return endPointY;
    }

    public void setEndPointY(double endPointY) {
        this.endPointY = endPointY;
    }
}
