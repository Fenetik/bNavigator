package com.wienerlinienproject.bac.bnavigator.Data;

import com.estimote.indoorsdk_module.cloud.Location;
import java.util.HashMap;
import java.util.List;

public class LocationObject {

    private String name;

    private double width; //in Meter
    private double height; //in Meter
    //Referenzpunkt mit Koordinaten von MAP in Metern
    private double startPointX;
    private double startPointY;

    private Location cloudLocation;

    // oben, unten, links, rechts
    private HashMap<LocationObject, Door> neighboursList;

    public LocationObject(String name) {
        this.name = name;
    }

    public LocationObject(String name, Location cloudLocation){
        this.name = name;
        this.cloudLocation = cloudLocation;
    }

    public LocationObject(String name, double width, double height, double startPointX, double startPointY, HashMap neighboursList){
        this.name = name;
        this.width = width;
        this.height = height;
        this.startPointX = startPointX;
        this.startPointY = startPointY;
        this.neighboursList = neighboursList;
    }

    public HashMap<LocationObject,Door> getNeighboursList() {
        return neighboursList;
    }

    public void setNeighboursList(HashMap<LocationObject, Door> neighboursList) {
        this.neighboursList = neighboursList;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Location getCloudLocation() {
        return cloudLocation;
    }

    public void setCloudLocation(Location cloudLocation) {
        this.cloudLocation = cloudLocation;
    }
}