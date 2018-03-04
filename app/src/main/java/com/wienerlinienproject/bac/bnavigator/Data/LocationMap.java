package com.wienerlinienproject.bac.bnavigator.Data;

import java.util.HashMap;
import java.util.Map;

//TODO wo wirklich intitialisieren? ws beaconservice => mittels broadcast an mainactivity schicken?
//Stores all Location Objects
public class LocationMap {

    //die Location in der sich der User gerade befindet
    private String activeLocation;
    //Key should be the locationName stored in the Cloud
    private Map<String,LocationObject> locations;

    public LocationMap(){
        locations = new HashMap<>();
    }

    public void addLocation(String name, LocationObject locationObject){

        locations.put(name, locationObject);
    }
    public String getActiveLocation() {
        return activeLocation;
    }

    public void setActiveLocation(String activeLocation) {
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