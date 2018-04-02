package  com.wienerlinienproject.bac.bnavigator.Service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.estimote.coresdk.cloud.api.CloudCallback;
import com.estimote.coresdk.common.exception.EstimoteCloudException;

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


import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
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
import java.util.stream.Collectors;

public class BeaconService extends Service implements BeaconConsumer {

    private Map<String, List<String>> PLACES_BY_BEACONS;
    private String destination = "nats--flur";

    private BeaconManager beaconManager;;
    private Region region;
    private IndoorCloudManager cloudManager;
    private ScanningIndoorLocationManager indoorManager;
    private LocationPosition position;
    private Location location;
    private List<LocationObject> allLocations;
    private LocationObject currentLocation;
    private LocationMap locationMap;
    private int locationCount =0;

    // TODO: Vermutung - alle IndoorManager & cloudManager usw. einfach rauslöschen - und die Anzeige je nach hardcoded Raum anzeigen wsl.

    private final IBinder mBinder = new BeaconBinder();
    @Override
    public void onCreate() {
        super.onCreate();

        region = new Region("Region", null, null, null);
        beaconManager = BeaconManager.getInstanceForApplication(this);

        cloudManager = new IndoorCloudManagerFactory().create(this);
        allLocations = new ArrayList<>();
        //TODO: einfach so Methode aufrufen, die Beacons den Räumen zuordnet
        defineLocations();
        /**cloudManager.getAllLocations(new CloudCallback<List<Location>>() {
            @Override
            public void success(List<Location> locations) {

                for (final Location location:locations) {

                    BeaconService.this.location = location;
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

    }

    public LocationObject getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(LocationObject newCurrent) {
        Log.d("currentLoc",  "called new currentlocation "+newCurrent.getName());
        this.currentLocation = newCurrent;
        locationMap.setActiveLocation(newCurrent.getName());
        locationChanged();
    }

    private void locationChanged(){

        indoorManager = new IndoorLocationManagerBuilder(BeaconService.this, currentLocation.getCloudLocation()).withDefaultScanner().build();
        indoorManager.setOnPositionUpdateListener(new OnPositionUpdateListener() {
            @Override
            public void onPositionUpdate(LocationPosition position) {

                // IndoorView UpdatePosition "zu langsam"?
                if (currentLocation != null) {
                    Log.d("locationManager", "Got position: " + position.getX() + ", " + position.getY() + ": "+currentLocation.getName());
                    BeaconService.this.position = position;
                    Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_BeaconService);
                    broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM, position.getX() + "," +
                            position.getY() + "," + currentLocation.getStartPointX() + "," + currentLocation.getStartPointY() + "," + currentLocation.getName());
                    sendBroadcast(broadcast);
                }
            }

            @Override
            public void onPositionOutsideLocation() {
                BeaconService.this.position = null;
            }
        });

        indoorManager.startPositioning();
    }

    //TODO: Beacons anpassen (BluetoothName bzw. BluetoothAddress angeben)
    private void defineLocations(){
        Log.d("defineLocations","called");
        locationMap = new LocationMap();
        Map<String, List<String>> placesByBeacons = new HashMap<>();
        for (LocationObject location : allLocations) {
            HashMap<Door, LocationObject> neighboursList;
            Door bottomDoor = null;
            Log.d("defineLocations",location.getName());
            switch (location.getName()) {
                case "nats--kitchen-5h5":
                    Log.d("defineLocations",location.getName());
                    location.setHeight(5.5);
                    location.setWidth(5.0);
                    location.setStartPointX(0.0);
                    location.setStartPointY(5.0);
                    bottomDoor = new Door("bottom", 2.5, 5.5, 3.0, 5.5);
                    neighboursList = new HashMap<>();
                    neighboursList.put(bottomDoor, new LocationObject("nats--room-p7q"));
                    location.setNeighboursList(neighboursList);
                    locationMap.addLocation("nats--kitchen-5h5", location);
                    break;
                case "nats--flur":
                    Log.d("defineLocations",location.getName());
                    location.setHeight(2.0);
                    location.setWidth(1.0);
                    location.setStartPointX(2.5);
                    location.setStartPointY(2.5);
                    setCurrentLocation(location);
                    bottomDoor = new Door("bottom", 0.1, 2.0, 0.6, 2.0);
                    neighboursList = new HashMap<>();
                    neighboursList.put(bottomDoor, new LocationObject("nats--room-p7q"));
                    location.setNeighboursList(neighboursList);
                    placesByBeacons.put("Beacon1", new ArrayList<String>() {{
                        add(currentLocation.getName());
                    }});
                    placesByBeacons.put("Beacon1", new ArrayList<String>() {{
                        add(currentLocation.getName());
                    }});
                    placesByBeacons.put("Beacon1", new ArrayList<String>() {{
                        add(currentLocation.getName());
                    }});
                    locationMap.addLocation("nats--flur", location);
                    break;
                case "nats--room-p7q":
                    // 2:1 & 2:2 sind fix drinnen
                    Log.d("defineLocations",location.getName());
                    location.setHeight(6.0);
                    location.setWidth(5.0);
                    location.setStartPointX(0.0);
                    location.setStartPointY(0.0);
                    Door topDoor = new Door("top", 2.5, 5.5, 3.0, 5.5);
                    neighboursList = new HashMap<>();
                    neighboursList.put(topDoor, new LocationObject("nats--kitchen-5h5"));
                    location.setNeighboursList(neighboursList);
                    List<String> list = new ArrayList<>();
                    list.add(currentLocation.getName());
                    list.add(location.getName());
                    placesByBeacons.put("Beacon1", list);
                    placesByBeacons.put("Beacon1", new ArrayList<String>() {{
                        add("nats--room-p7q");
                    }});
                    placesByBeacons.put("Beacon1", new ArrayList<String>() {{
                        add("nats--room-p7q");
                    }});
                    locationMap.addLocation("nats--room-p7q", location);
                    break;
            }


        }
        Log.d("defineLocation", "defineLocations done");
        PLACES_BY_BEACONS = Collections.unmodifiableMap(placesByBeacons);
    }

    private void run() {
        Log.d("BeaconService", "run");

        try {
            beaconManager.startRangingBeaconsInRegion(region);
            beaconManager.startMonitoringBeaconsInRegion(region);
            Log.d("ranging","started");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        run();
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        try {
            beaconManager.stopMonitoringBeaconsInRegion((region));
            beaconManager.stopRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        indoorManager.stopPositioning();
        //Service wird sicher beendet sobald sich jemand unbindet
        this.stopSelf();

        return super.onUnbind(intent);
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    HashMap<Beacon, Double> beaconDistances = new HashMap<>();
                    for (Beacon beacon: beacons) {
                        beaconDistances.put(beacon, beacon.getDistance());
                    }
                    sortByValue(beaconDistances);
                    Beacon current = beaconDistances.entrySet().iterator().next().getKey();
                    Log.i("Ranging", "The closest beacon is "+current.getBluetoothAddress());
                    String currentLocation = placesNearBeacon(current).get(0);
                    Log.i("Ranging", "The closest location is " + currentLocation);
                    //TODO: je nach Beacon currentLocation setzen
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
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

    public class BeaconBinder extends Binder {
        public BeaconService getService(){
            return BeaconService.this;
        }
    }

    public Location getLocation() {
        return location;
    }

    //TODO: überlegen ob BluetoothName oder BluetoothAddress
    private List<String> placesNearBeacon(Beacon beacon) {
        if(PLACES_BY_BEACONS!= null){
            String beaconKey = beacon.getBluetoothName();
            if (PLACES_BY_BEACONS.containsKey(beaconKey)) {
                return PLACES_BY_BEACONS.get(beaconKey);
            }
        }
        return Collections.emptyList();
    }

    private String navigate(Beacon beacon, String destination){

        if (destination.equals(placesNearBeacon(beacon).get(0)))
            return "Arrived at destination";
        return "Go to: "+placesNearBeacon(beacon).get(1);
    }
}