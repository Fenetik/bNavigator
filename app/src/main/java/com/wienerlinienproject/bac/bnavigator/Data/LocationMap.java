package com.wienerlinienproject.bac.bnavigator.Data;

import android.location.Location;

import java.util.HashMap;
import java.util.Map;

//Stores all Location Objects
public class LocationMap {

    //die Location in der sich der User gerade befindet
    private LocationObject activeLocation;
    //Key should be the locationName stored in the Cloud
    private Map<String,LocationObject> locations;

    public LocationMap(){
        locations = new HashMap<>();
    }

    public void addLocation(String name, LocationObject locationObject){

        locations.put(name, locationObject);
    }
    public LocationObject getActiveLocation() {
        return activeLocation;
    }

    public void setActiveLocation(LocationObject activeLocation) {
        this.activeLocation = activeLocation;
    }

    public Map<String, LocationObject> getLocations() {
        return locations;
    }

    public void setLocations(Map<String, LocationObject> locations) {
        this.locations = locations;
    }

    public LocationObject getLocationByName(String name){
        return locations.get(name);
    }
}