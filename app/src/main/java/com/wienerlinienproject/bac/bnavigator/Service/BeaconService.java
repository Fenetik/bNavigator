package  com.wienerlinienproject.bac.bnavigator.Service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.estimote.cloud_plugin.common.EstimoteCloudCredentials;
import com.estimote.coresdk.observation.region.beacon.BeaconRegion;
import com.estimote.coresdk.service.BeaconManager;
import com.estimote.indoorsdk.IndoorLocationManagerBuilder;
import com.estimote.indoorsdk_module.algorithm.OnPositionUpdateListener;
import com.estimote.indoorsdk_module.algorithm.ScanningIndoorLocationManager;
import com.estimote.indoorsdk_module.cloud.CloudCallback;
import com.estimote.indoorsdk_module.cloud.EstimoteCloudException;
import com.estimote.indoorsdk_module.cloud.IndoorCloudManager;
import com.estimote.indoorsdk_module.cloud.IndoorCloudManagerFactory;
import com.estimote.indoorsdk_module.cloud.Location;
import com.estimote.indoorsdk_module.cloud.LocationPosition;
import com.estimote.internal_plugins_api.cloud.CloudCredentials;
import com.estimote.internal_plugins_api.scanning.Beacon;
import com.estimote.internal_plugins_api.scanning.BluetoothScanner;
import com.estimote.internal_plugins_api.scanning.EstimoteTelemetryFull;
import com.estimote.internal_plugins_api.scanning.ScanHandler;
import com.estimote.scanning_sdk.api.EstimoteBluetoothScannerFactory;
import com.wienerlinienproject.bac.bnavigator.Data.Door;
import com.wienerlinienproject.bac.bnavigator.Data.LocationMap;
import com.wienerlinienproject.bac.bnavigator.Data.LocationObject;
import com.wienerlinienproject.bac.bnavigator.Presentation.MainActivity;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class BeaconService extends Service {


    private  Map<String, List<String>> PLACES_BY_BEACONS;
    private String destination = "kitchen-2s1";
    private IndoorCloudManager cloudManager;
    private ScanningIndoorLocationManager indoorManager;
    private final CloudCredentials cloudCredentials = new EstimoteCloudCredentials("natasa-nikic-info-s-your-o-e6g",
                                                                      "b437d62cd736bece0f9a475fe861e3d4");

    /**
     * room: room-84l
     *  - 1.1-beetroot: d2e5a99f0a6157b4e89bb709977d6716
     *  - 2.1-lemon: b8d5a0caabab6dd856073a3a37d99526
     *  - 3.1-candy: 440bb6986feb036ae8a78eeca764123c
     *  - 1.2-beetroot: e0dfa3041770efc1ddeb00687bac2c39 -> door
     *
     * kitchen: kitchen-2s1
     *  - 1.3-beetroot: 8e43145d98d7344f38371cfc76f37230
     *  - 3.2-candy: 41e1ac7c775ac9ab3c0f5f556ed8de07
     *  - 2.2-lemon: 93850bb0152d1f4ed94fc09fc3f6fd06
     *  - 2.3-lemon: 13b3a6c5dd201ca168b4ed4c94a0492f -> door
     *
     */



    private LocationPosition position;
    private Location activeLocation;
    private Location locationFlur;
    private Location locationKitchen;
    private BeaconManager beaconManager;
    private BeaconRegion region;
    private int tempcount = 1;


    private List<LocationObject> allLocations;
    private LocationObject currentLocation;
    private LocationMap locationMap;

    private final IBinder mBinder = new BeaconBinder();

    public BeaconService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //TODO alle locations laden
        beaconManager = new BeaconManager(getApplicationContext());
        region = new BeaconRegion("ranged region", null, null, null);

        cloudManager = new IndoorCloudManagerFactory().create(this,cloudCredentials);
        allLocations = new ArrayList<>();


        /*cloudManager.getAllLocations(new CloudCallback<List<Location>>() {
            @Override
            public void success(List<Location> locations) {

                for (final Location location:locations) {

                    BeaconService.this.activeLocation = location;
                    LocationObject newLocationObj = new LocationObject(location.getIdentifier());
                    newLocationObj.setCloudLocation(location);
                    allLocations.add(newLocationObj);

                    Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_getLocation);
                    broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM, "");
                    sendBroadcast(broadcast);

                    Log.d("cloudService","Got location: " + location.getIdentifier());
                }

                defineLocations();
            }

            @Override
            public void failure(EstimoteCloudException serverException) {
                Log.d("cloudService","Getting Location from Cloud failed: "+ serverException.toString());
                Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_BeaconService);
                broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM,
                "Getting Location from Cloud failed");
                sendBroadcast(broadcast);
            }
        });*/


        cloudManager.getLocation("room-84l", new CloudCallback<Location>() {
            @Override
            public void success(final Location location) {

                BeaconService.this.activeLocation = location;
                BeaconService.this.locationKitchen = location;

                Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_getLocation);
                broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM, "");
                sendBroadcast(broadcast);

                LocationObject newLocationObj = new LocationObject(location.getIdentifier());
                newLocationObj.setCloudLocation(location);
                allLocations.add(newLocationObj);

                defineLocations();
                Log.d("cloudService", "got location");
                setCurrentLocation(newLocationObj);
            }

            @Override
            public void failure(EstimoteCloudException serverException) {
                Log.d("cloudService","Getting Location from Cloud failed: "+ serverException.toString()+"room-84l");
               /* Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_BeaconService);
                broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM,
                        "Getting Location from Cloud failed");
                sendBroadcast(broadcast);*/
                }
        });

        cloudManager.getLocation("kitchen-2s1", new CloudCallback<Location>() {
            @Override
            public void success(final Location location) {

                BeaconService.this.activeLocation = location;
                BeaconService.this.locationKitchen = location;

                Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_getLocation);
                broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM, "");
                sendBroadcast(broadcast);

                LocationObject newLocationObj = new LocationObject(location.getIdentifier());
                newLocationObj.setCloudLocation(location);
                allLocations.add(newLocationObj);
                defineLocations();

                Log.d("cloudService", "got location");
                setCurrentLocation(newLocationObj);
            }

            @Override
            public void failure(EstimoteCloudException serverException) {
                Log.d("cloudService","Getting Location from Cloud failed: "+ serverException.toString()+"room-84l");
               /* Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_BeaconService);
                broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM,
                        "Getting Location from Cloud failed");
                sendBroadcast(broadcast);*/
            }
        });


        /*beaconManager.setRangingListener(new BeaconManager.BeaconRangingListener() {
            @Override
            public void onBeaconsDiscovered(BeaconRegion beaconRegion, List<com.estimote.coresdk.recognition.packets.Beacon> beacons) {
                Log.d("onBeaconDiscovered", "vorm IF");
                if (!beacons.isEmpty()) {
                    com.estimote.coresdk.recognition.packets.Beacon nearestBeacon = beacons.get(0);
                    List<String> places = placesNearBeacon(nearestBeacon);
                    String output = "";
                    if(!places.isEmpty()){
                        if(!places.get(0).equals(currentLocation.getName())) {
                            if(indoorManager != null)
                                indoorManager.stopPositioning();
                            setCurrentLocation(locationMap.getLocationByName(places.get(0)));
                        }
                        //wenn nur 1 place in der Liste ist
                        if(places.size() == 1){
                            output += places.get(0) + " (Nothing to navigate.)";
                            Log.d("RangingListener", places.get(0));
                        }else{
                            output += places.get(0) + " "+navigate(nearestBeacon, destination);
                            Log.d("RangingListener", places.get(0));
                        }
                    }else {
                        output+= "No new places";
                        Log.d("RangingListener", "No new places");
                    }

                    /*Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_BeaconService);
                    broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM, output);
                    sendBroadcast(broadcast);
                }else{
                    //dafuq
                }
            }

        });*/

    }

    private void defineLocations(){
        Log.d("defineLocations","called");
        locationMap = new LocationMap();
        Map<String, List<String>> placesByBeacons = new HashMap<>();
        for (LocationObject location : allLocations) {
            HashMap<Door, LocationObject> neighboursList;
            Door bottomDoor = null;
            switch (location.getName()) {
                case "kitchen-2s1":
                    Log.d("defineLocations",location.getName());
                    location.setHeight(5.0);
                    location.setWidth(3.5);
                    location.setStartPointX(2.0);
                    location.setStartPointY(0.75);
                    bottomDoor = new Door("bottom", 2.5, 5.5, 3.0, 5.5);
                    neighboursList = new HashMap<>();
                    neighboursList.put(bottomDoor, new LocationObject("room-84l"));
                    location.setNeighboursList(neighboursList);
                    placesByBeacons.put("8e43145d98d7344f38371cfc76f37230", new ArrayList<String>() {{
                        add("kitchen-2s1");
                    }});
                    placesByBeacons.put("41e1ac7c775ac9ab3c0f5f556ed8de07", new ArrayList<String>() {{
                        add("kitchen-2s1");
                    }});
                    placesByBeacons.put("93850bb0152d1f4ed94fc09fc3f6fd06", new ArrayList<String>() {{
                        add("kitchen-2s1");
                    }});
                    placesByBeacons.put("13b3a6c5dd201ca168b4ed4c94a0492f", new ArrayList<String>() {{
                        add("kitchen-2s1");
                        add("room-84l");
                    }});
                    locationMap.addLocation("kitchen-2s1", location);
                    break;
                case "room-84l":
                    Log.d("defineLocations",location.getName());
                    location.setHeight(6.0);
                    location.setWidth(5.0);
                    location.setStartPointX(0.5);
                    location.setStartPointY(7.8);
                    Door topDoor = new Door("top", 2.5, 5.5, 3.0, 5.5);
                    neighboursList = new HashMap<>();
                    neighboursList.put(topDoor, new LocationObject("kitchen-2s1"));
                    location.setNeighboursList(neighboursList);
                    placesByBeacons.put("d2e5a99f0a6157b4e89bb709977d6716", new ArrayList<String>() {{
                        add("room-84l");
                    }});
                    placesByBeacons.put("b8d5a0caabab6dd856073a3a37d99526", new ArrayList<String>() {{
                        add("room-84l");
                    }});
                    placesByBeacons.put("440bb6986feb036ae8a78eeca764123c", new ArrayList<String>() {{
                        add("room-84l");
                    }});
                    placesByBeacons.put("e0dfa3041770efc1ddeb00687bac2c39", new ArrayList<String>() {{
                        add("room-84l");
                        add("kitchen-2s1");
                    }});
                    locationMap.addLocation("room-84l", location);
                    break;
            }


        }
        Log.d("defineLocation", "defineLocations done");
        PLACES_BY_BEACONS = Collections.unmodifiableMap(placesByBeacons);
    }

    public void setCurrentLocation(LocationObject newCurrent) {
        Log.d("currentLoc",  "called new currentlocation "+newCurrent.getName());
        this.currentLocation = newCurrent;
        this.activeLocation = currentLocation.getCloudLocation();
        locationMap.setActiveLocation(newCurrent.getName());
        indoorManagerInit(activeLocation);
    }

    private void indoorManagerInit(final Location locationToRange) {

        indoorManager = new IndoorLocationManagerBuilder(BeaconService.this,locationToRange,cloudCredentials).withDefaultScanner().build();
        indoorManager.setOnPositionUpdateListener(new OnPositionUpdateListener() {

            @Override
            public void onPositionUpdate(LocationPosition position) {

                Log.d("locationManager", "Got position: " + position.getX() + ", " + position.getY()+" Location:"+locationToRange.getIdentifier() +
                " Count:" + tempcount);
                BeaconService.this.position = position;
                Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_BeaconService);
                broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM, currentLocation.getStartPointX()+","+currentLocation.getStartPointX()+","+position.getX() + "," +
                        position.getY()+","+locationToRange.getIdentifier());
                sendBroadcast(broadcast);


                BeaconService.this.setCount(BeaconService.this.tempcount+1);

               /* if(BeaconService.this.tempcount > 4){
                    BeaconService.this.setCount(1);
                    indoorManager.stopPositioning();
                    if(locationToRange.getName().equals(locationKitchen.getName())){
                        Log.d("Locationchange","Changing Location to Flur");
                        indoorManagerInit(locationFlur);
                    }else{
                        Log.d("Locationchange","Changing Location to Kitchen");
                        indoorManagerInit(locationKitchen);
                    }

                } */
            }
            @Override
            public void onPositionOutsideLocation() {
                BeaconService.this.position = null;
            }
        });

        indoorManager.startPositioning();

    }

    private void run(){
        Log.d("BeaconService", "run");
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override public void onServiceReady() {
                beaconManager.startRanging(region);
                beaconManager.startLocationDiscovery();
                Log.d("ranging","started");
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        run();
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        beaconManager.stopRanging(region);
        beaconManager.disconnect();
        indoorManager.stopPositioning();
        //Service wird sicher beendet sobald sich jemand unbindet
        this.stopSelf();

        return super.onUnbind(intent);
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
        Collections.sort( list, new Comparator<Map.Entry<K, V>>() {
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return (o1.getValue()).compareTo( o2.getValue() );
            }
        });

        Map<K, V> result = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private List<String> placesNearBeacon(com.estimote.coresdk.recognition.packets.Beacon beacon) {
        if(PLACES_BY_BEACONS!= null){
            String beaconKey = beacon.getUniqueKey();
            if (PLACES_BY_BEACONS.containsKey(beaconKey)) {
                return PLACES_BY_BEACONS.get(beaconKey);
            }
        }
        return Collections.emptyList();
    }

    /*private String navigate(com.estimote.coresdk.recognition.packets.Beacon beacon, String destination){

        if (destination.equals(placesNearBeacon(beacon).get(0)))
            return "Arrived at destination";
        return "Go to: "+placesNearBeacon(beacon).get(1);
    }*/

    public class BeaconBinder extends Binder {

       public BeaconService getService(){
            return BeaconService.this;
        }
    }

    public void setCount(int count) {
        this.tempcount = count;
    }
}
