package  com.wienerlinienproject.bac.bnavigator.Service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.estimote.coresdk.cloud.api.CloudCallback;
import com.estimote.coresdk.common.exception.EstimoteCloudException;
import com.estimote.coresdk.observation.region.beacon.BeaconRegion;
import com.estimote.coresdk.recognition.packets.Beacon;
import com.estimote.coresdk.service.BeaconManager;
import com.estimote.indoorsdk.algorithm.IndoorLocationManagerBuilder;
import com.estimote.indoorsdk.algorithm.OnPositionUpdateListener;
import com.estimote.indoorsdk.algorithm.ScanningIndoorLocationManager;
import com.estimote.indoorsdk.cloud.IndoorCloudManager;
import com.estimote.indoorsdk.cloud.IndoorCloudManagerFactory;
import com.estimote.indoorsdk.cloud.Location;
import com.estimote.indoorsdk.cloud.LocationPosition;
import com.wienerlinienproject.bac.bnavigator.Data.Door;
import com.wienerlinienproject.bac.bnavigator.Data.LocationMap;
import com.wienerlinienproject.bac.bnavigator.Data.LocationObject;
import com.wienerlinienproject.bac.bnavigator.Presentation.MainActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeaconService extends Service {

    private Map<String, List<String>> PLACES_BY_BEACONS;
    private String destination = "Nats' room";

    private BeaconManager beaconManager;
    private BeaconRegion region;

    private IndoorCloudManager cloudManager;
    private ScanningIndoorLocationManager indoorManager;
    private LocationPosition position;
    private Location location;
    private List<LocationObject> allLocations;
    private LocationObject currentLocation;
    private LocationMap locationMap;

    private final IBinder mBinder = new BeaconBinder();

    public BeaconService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        beaconManager = new BeaconManager(getApplicationContext());
        region = new BeaconRegion("ranged region", null, null, null);

        cloudManager = new IndoorCloudManagerFactory().create(this);

        beaconManager.setRangingListener(new BeaconManager.BeaconRangingListener() {
            @Override
            public void onBeaconsDiscovered(BeaconRegion region, List<Beacon> list) {
                if (!list.isEmpty()) {
                    Beacon nearestBeacon = list.get(0);
                    List<String> places = placesNearBeacon(nearestBeacon);
                    String output = "";
                    if(!places.isEmpty()){
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
                    sendBroadcast(broadcast);*/
                    Log.d("moni","sending msg:" +output);
                }else{
                    //dafuq
                }
            }
        });
        allLocations = new ArrayList<>();
        cloudManager.getAllLocations(new CloudCallback<List<Location>>() {
            @Override
            public void success(List<Location> locations) {
                for (final Location location:locations) {
                    BeaconService.this.location = location;
                    LocationObject newLocationObj = new LocationObject(location.getName());
                    allLocations.add(newLocationObj);

                    Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_getLocation);
                    broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM, "");
                    sendBroadcast(broadcast);

                    Log.d("cloudService","Got location: " + location.getName());
                    indoorManager = new IndoorLocationManagerBuilder(BeaconService.this,location).withDefaultScanner().build();
                    indoorManager.setOnPositionUpdateListener(new OnPositionUpdateListener() {
                        @Override
                        public void onPositionUpdate(LocationPosition position) {

                            // IndoorView UpdatePosition "zu langsam"?

                            Log.d("locationManager", "Got position: " + position.getX() + ", " + position.getY());
                            BeaconService.this.position = position;
                            Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_BeaconService);
                            broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM, position.getX() + "," +
                                    position.getY()+","+currentLocation.getStartPointX() + "," +currentLocation.getStartPointY() + "," +location.getName());
                            sendBroadcast(broadcast);
                        }

                        @Override
                        public void onPositionOutsideLocation() {
                            BeaconService.this.position = null;
                        }
                    });

                    indoorManager.startPositioning();
                }
            }

            @Override
            public void failure(EstimoteCloudException serverException) {
                Log.d("cloudService","Getting Location from Cloud failed: "+ serverException.toString());
                Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_BeaconService);
                broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM,
                        "Getting Location from Cloud failed");
                sendBroadcast(broadcast);
            }
        });
        if (!allLocations.isEmpty() && allLocations != null) {
            defineLocations();
        }
    }

    public LocationObject getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(LocationObject currentLocation) {
        this.currentLocation = currentLocation;
        locationMap.setActiveLocation(currentLocation.getName());
    }

    private void defineLocations(){

        locationMap = new LocationMap();
        Map<String, List<String>> placesByBeacons = new HashMap<>();
        for (LocationObject location : allLocations) {
            HashMap<Door, LocationObject> neighboursList;
            switch (location.getName()) {
                /*case "Nats’ kitchen":
                    location.setHeight(5.5);
                    location.setWidth(5.0);
                    location.setStartPointX(0.0);
                    location.setStartPointY(0.0);
                    Door bottomDoor = new Door("bottom", 2.5, 5.5, 3.0, 5.5);
                    neighboursList = new HashMap<>();
                    neighboursList.put(bottomDoor, new LocationObject("Nats’ room"));
                    location.setNeighboursList(neighboursList);

                    placesByBeacons.put("1:2", new ArrayList<String>() {{
                        add(currentLocation.getName());
                    }});
                    placesByBeacons.put("3:1", new ArrayList<String>() {{
                        add(currentLocation.getName());
                    }});
                    placesByBeacons.put("3:2", new ArrayList<String>() {{
                        add(currentLocation.getName());
                    }});
                    placesByBeacons.put("1:1", new ArrayList<String>() {{
                        add(currentLocation.getName());
                    }});
                    placesByBeacons.put("2:2", new ArrayList<String>() {{
                        add(currentLocation.getName());
                    }});
                    locationMap.addLocation("Nats' kitchen", location);
                    break;*/
                case "Nats' flur":
                    location.setHeight(2.0);
                    location.setWidth(1.0);
                    location.setStartPointX(2.5);
                    location.setStartPointY(5.5);
                    setCurrentLocation(location);
                    Door bottomDoor = new Door("bottom", 0.1, 2.0, 0.6, 2.0);
                    neighboursList = new HashMap<>();
                    neighboursList.put(bottomDoor, new LocationObject("Nats’ flur"));
                    location.setNeighboursList(neighboursList);
                    placesByBeacons.put("3:1", new ArrayList<String>() {{
                        add(currentLocation.getName());
                    }});
                    placesByBeacons.put("1:1", new ArrayList<String>() {{
                        add(currentLocation.getName());
                    }});
                    placesByBeacons.put("1:2", new ArrayList<String>() {{
                        add(currentLocation.getName());
                    }});
                    placesByBeacons.put("3:2", new ArrayList<String>() {{
                        add(currentLocation.getName());
                    }});
                    locationMap.addLocation("Nats' room", location);
                    break;
                case "Nats’ room":
                    // 2:1 & 2:2 sind fix drinnen
                    location.setHeight(6.0);
                    location.setWidth(5.0);
                    location.setStartPointX(0.0);
                    location.setStartPointY(7.5);
                    Door topDoor = new Door("top", 2.5, 5.5, 3.0, 5.5);
                    neighboursList = new HashMap<>();
                    neighboursList.put(topDoor, new LocationObject("Nats’ kitchen"));
                    location.setNeighboursList(neighboursList);
                    List<String> list = new ArrayList<>();
                    list.add(currentLocation.getName());
                    list.add(location.getName());
                    placesByBeacons.put("1:2", list);
                    locationMap.addLocation("Nats' room", location);
                    break;
            }


        }
        PLACES_BY_BEACONS = Collections.unmodifiableMap(placesByBeacons);
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

        beaconManager.disconnect();
        indoorManager.stopPositioning();
        //Service wird sicher beendet sobald sich jemand unbindet
        this.stopSelf();

        return super.onUnbind(intent);
    }

    public class BeaconBinder extends Binder {
        public BeaconService getService(){
            return BeaconService.this;
        }
    }

    public Location getLocation() {
        return location;
    }

    private List<String> placesNearBeacon(Beacon beacon) {
        String beaconKey = String.format("%d:%d", beacon.getMajor(), beacon.getMinor());
        if (PLACES_BY_BEACONS.containsKey(beaconKey)) {
            return PLACES_BY_BEACONS.get(beaconKey);
        }
        return Collections.emptyList();
    }

    private String navigate(Beacon beacon, String destination){

        if (destination.equals(placesNearBeacon(beacon).get(0)))
            return "Arrived at destination";
        return "Go to: "+placesNearBeacon(beacon).get(1);
    }
}
/*
 ------- old cloudManager -> 1 Raum reinladen
        cloudManager.getLocation("nats--kitchen", new CloudCallback<Location>() {
            @Override
            public void success(final Location location) {

                BeaconService.this.location = location;

                Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_getLocation);
                broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM, "");
                sendBroadcast(broadcast);

                Log.d("cloudService","Got location: " + location.getName());
                indoorManager = new IndoorLocationManagerBuilder(BeaconService.this,location).withDefaultScanner().build();
                indoorManager.setOnPositionUpdateListener(new OnPositionUpdateListener() {
                    @Override
                    public void onPositionUpdate(LocationPosition position) {

                        // IndoorView UpdatePosition "zu langsam"?

                        Log.d("locationManager", "Got position: " + position.getX() + ", " + position.getY());
                        BeaconService.this.position = position;
                        Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_BeaconService);
                        broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM, position.getX() + "," +
                                position.getY()+","+location.getName());
                        sendBroadcast(broadcast);
                    }

                    @Override
                    public void onPositionOutsideLocation() {
                        BeaconService.this.position = null;
                    }
                });

                indoorManager.startPositioning();

            }

            @Override
            public void failure(EstimoteCloudException serverException) {
                Log.d("cloudService","Getting Location from Cloud failed: "+ serverException.toString());
                Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_BeaconService);
                broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM,
                        "Getting Location from Cloud failed");
                sendBroadcast(broadcast);            }
        });*/
