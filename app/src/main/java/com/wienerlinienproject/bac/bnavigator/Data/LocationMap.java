package com.wienerlinienproject.bac.bnavigator.Data;



import java.util.HashMap;
import java.util.Map;


//Stores all Location Objects
public class LocationMap {
    //die Location in der sich der User gerade befindet
    private String activeLocation;
    //Key should be the locationName stored in the Cloud
    private Map<String,LocationObject> locations = new HashMap();

    public LocationMap(){
        //TODO intitialize LocationObjects and write to Map
    }
}
