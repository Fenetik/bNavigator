package  com.wienerlinienproject.bac.bnavigator.Service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.estimote.cloud_plugin.common.EstimoteCloudCredentials;
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
import com.wienerlinienproject.bac.bnavigator.Data.Door;
import com.wienerlinienproject.bac.bnavigator.Data.LocationMap;
import com.wienerlinienproject.bac.bnavigator.Data.LocationObject;
import com.wienerlinienproject.bac.bnavigator.Presentation.MainActivity;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;


public class BeaconService extends Service implements BeaconConsumer,RangeNotifier{


    private  Map<String, List<String>> PLACES_BY_BEACONS;
    private String destination = "kitchen-2s1";
    private IndoorCloudManager cloudManager;
    private ScanningIndoorLocationManager indoorManager = null;
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
    // private BeaconManager beaconManager;
    //private BeaconRegion region;
    private int tempcount = 1;


    private List<LocationObject> allLocations;
    private LocationObject currentLocation;
    private LocationMap locationMap;

    private final IBinder mBinder = new BeaconBinder();

    //altbeacon manager
    private BeaconManager altBeaconManager;
    private Region region;

    //TODO in eine Map counts wo für alle locations ide counts drinnen sind damit das nicht statisch ist
    private int countRoom = 0;
    private int countKitchen = 0;

    public BeaconService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //TODO alle locations laden also getAll Locations verwenden
        // beaconManager = new BeaconManager(getApplicationContext());
        //region = new BeaconRegion("ranged region", null, null, null);

        cloudManager = new IndoorCloudManagerFactory().create(this,cloudCredentials);
        allLocations = new ArrayList<>();
        locationMap = new LocationMap();

        altBeaconManager = BeaconManager.getInstanceForApplication(this.getApplicationContext());
        altBeaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19"));

        Log.d("altbeaconRanging","Manager bound");

        cloudManager.getAllLocations(new CloudCallback<List<Location>>() {
            @Override
            public void success(List<Location> locations) {

                for (final Location location:locations) {

                    //BeaconService.this.activeLocation = location;
                    LocationObject newLocationObj = new LocationObject(location.getIdentifier(),location);
                    allLocations.add(newLocationObj);

                    if(location.getIdentifier().equals("room-84l")){
                        Log.d("cloudService","added Location:" +location.getIdentifier());
                        locationMap.addLocation("room-841",newLocationObj);
                    }

                    if(location.getIdentifier().equals("kitchen-2s1")){
                        Log.d("cloudService","added Location:" +location.getIdentifier());
                        locationMap.addLocation("kitchen-2s1", newLocationObj);
                    }


                    //locationMap.addLocation(newLocationObj.getName(),newLocationObj);

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
               /* Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_BeaconService);
                broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM,
                "Getting Location from Cloud failed");
                sendBroadcast(broadcast); */
            }
        });

        /*
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
                Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_BeaconService);
                broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM,
                        "Getting Location from Cloud failed");
                sendBroadcast(broadcast);
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
                Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_BeaconService);
                broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM,
                        "Getting Location from Cloud failed");
                sendBroadcast(broadcast);
            }
        });
        */
    }

    private void defineLocations(){
        Log.d("defineLocations","called");
        Map<String, List<String>> placesByBeacons = new HashMap<>();
        LocationObject lastDefinedLocation = null;
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
                    //TODO why new locationobject?
                    neighboursList.put(bottomDoor, locationMap.getLocationByName("room-84l"));
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
                    //locationMap.addLocation("kitchen-2s1", location);
                    lastDefinedLocation = location;
                    break;
                case "room-84l":
                    Log.d("defineLocations",location.getName());
                    location.setHeight(6.0);
                    location.setWidth(5.0);
                    location.setStartPointX(0.5);
                    location.setStartPointY(7.8);
                    Door topDoor = new Door("top", 2.5, 5.5, 3.0, 5.5);
                    neighboursList = new HashMap<>();
                    //TODO why new locationobject
                    neighboursList.put(topDoor, locationMap.getLocationByName("kitchen-2s1"));
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
                    lastDefinedLocation = location;
                    break;
            }


        }
        //so that a current location is set from the beginning on
        setCurrentLocation(lastDefinedLocation);
        Log.d("defineLocation", "defineLocations done");
        int i = 1;
        Map tempmap = locationMap.getLocations();
        Set tempset = tempmap.keySet();
        for(Object loc: tempset){
            Log.d("defineLocation","Locationmap Key "+i +": "+loc);
            Log.d("defineLocation","Locationmap Entry "+i +": "+locationMap.getLocationByName((String)loc).getName());
            i++;
        }

        //Ranging darf erst nach define location fertig ist beginnen
        altBeaconManager.bind(this);
        PLACES_BY_BEACONS = Collections.unmodifiableMap(placesByBeacons);
    }

    public void setCurrentLocation(LocationObject newCurrent) {
        Log.d("altbeaconRanging",  "called new currentlocation "+newCurrent.getName());
        Log.d("locationManager",  "AAAAAAAAAAAcalled new currentlocation "+newCurrent.getName());
        currentLocation = newCurrent;
        activeLocation = currentLocation.getCloudLocation();
        locationMap.setActiveLocation(newCurrent);
        //indoorManager.stopPositioning();
        indoorManagerInit(activeLocation);
    }

    private void indoorManagerInit(final Location locationToRange) {
        Log.d("locationManager","Init called");
        indoorManager = new IndoorLocationManagerBuilder(BeaconService.this,locationToRange,cloudCredentials).withDefaultScanner().build();
        indoorManager.setOnPositionUpdateListener(new OnPositionUpdateListener() {

            @Override
            public void onPositionUpdate(LocationPosition position) {
                //TODO schauen ob es immer der selbe indoorManager ist der nur schneller updates schickt => ja ist immer das selbe objekt
                if(locationToRange.getIdentifier().equals(currentLocation.getName())){
                    Log.d("locationManager", "Mgr:"+ indoorManager.toString().substring(30) +" PosUpdate:" + position.getX() + ", " + position.getY()+" Location:"+locationToRange.getIdentifier() +
                            " Count:" + tempcount);
                    BeaconService.this.position = position;
                    Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_BeaconService);
                    broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM, currentLocation.getStartPointX()+","+currentLocation.getStartPointY()+","+position.getX() + "," +
                            position.getY()+","+locationToRange.getIdentifier());
                    sendBroadcast(broadcast);


                    BeaconService.this.setCount(BeaconService.this.tempcount+1);
                }else{
                    Log.d("locationManager","dropped PosUpdate for "+ locationToRange.getIdentifier());
                }


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
//        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
//            @Override public void onServiceReady() {
//                beaconManager.startRanging(region);
//                beaconManager.startLocationDiscovery();
//                Log.d("ranging","started");
//            }
//        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        run();
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        try {

            indoorManager.stopPositioning();
            altBeaconManager.stopRangingBeaconsInRegion(region);
            altBeaconManager.unbind(this);

        } catch (RemoteException e) {
            e.printStackTrace();
        }

        //Service wird sicher beendet sobald sich jemand unbindet
        this.stopSelf();

        return super.onUnbind(intent);
    }


    @Override
    public void onBeaconServiceConnect() {
        region = new Region("all-beacons-region", null, null, null);
        try {
            altBeaconManager.startRangingBeaconsInRegion(region);
            Log.d("altbeaconRanging","Starting Ranging");


        } catch (Exception e) {
            e.printStackTrace();
        }
        altBeaconManager.setRangeNotifier(this);
    }


    //TODO eventuell die beacons in reagionen einteilen und die regionen dann gleich dem raum nennen
    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> collection, Region region) {
        Log.d("altbeaconRanging","didRangeBeaconsInReagion " + collection.size());
        Beacon nearestBeacon = null;
        for (Beacon beacon: collection) {
            //Log.d("altBeaconRanging",beacon.getId1()+" dist:" + beacon.getDistance());
            //if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x00) {
            // This is a Eddystone-UID frame
            if(nearestBeacon == null){
                nearestBeacon = beacon;
            }else{
                if(nearestBeacon.getDistance()> beacon.getDistance()){
                    nearestBeacon = beacon;
                }
            }
        }
        //manchmal ist die collection leer => nearestBeacon wäre null
        if(nearestBeacon != null){
            if (nearestBeacon.getId1().toString().startsWith("0xbbbb")){
                countRoom += 1;
            }else{
                countKitchen +=1;
            }
            if(countRoom >= 3){
                //room-841
                //room-841
                Log.d("altbeaconranging", "current:"+currentLocation.getName() + " vs:room-841 " + currentLocation.getName().equals("room-84l"));
                if(!currentLocation.getName().equals("room-84l")){
                    //TODO nullpointer bei change auf room
                    Log.d("altbeaconranging",locationMap.getLocationByName("room-841").getName());
                    setCurrentLocation(locationMap.getLocationByName("room-841"));
                }
                countKitchen = 0;
                countRoom =0;
            }else if(countKitchen >= 3){
                if(!currentLocation.getName().equals("kitchen-2s1")){
                    Log.d("altbeaconranging",locationMap.getLocationByName("kitchen-2s1").getName());
                    setCurrentLocation(locationMap.getLocationByName("kitchen-2s1"));
                }
                countKitchen = 0;
                countRoom =0;
            }
        }
        Log.d("altbeaconRanging","Current Loc:"+currentLocation.getName()+ " CountKitchen: " + countKitchen+ " CountRoom:"+ countRoom);
    }

   /* private List<String> placesNearBeacon(com.estimote.coresdk.recognition.packets.Beacon beacon) {
        if(PLACES_BY_BEACONS!= null){
            String beaconKey = beacon.getUniqueKey();
            if (PLACES_BY_BEACONS.containsKey(beaconKey)) {
                return PLACES_BY_BEACONS.get(beaconKey);
            }
        }
        return Collections.emptyList();
    }*/

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