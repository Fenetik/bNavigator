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
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;



public class BeaconService extends Service implements BeaconConsumer,RangeNotifier{

    private IndoorCloudManager cloudManager;
    private ScanningIndoorLocationManager indoorManager = null;
    private final CloudCredentials cloudCredentials = new EstimoteCloudCredentials("natasa-nikic-info-s-your-o-e6g",
            "b437d62cd736bece0f9a475fe861e3d4");

    private Location activeLocation;
    private LocationPosition position;
    private int tempcount = 1;


    private HashMap<String, Location> allLocations;
    private LocationMap locationMap;

    private final IBinder mBinder = new BeaconBinder();

    //altbeacon manager
    private BeaconManager altBeaconManager;
    private Region region;

    //TODO in eine Map counts wo für alle locations ide counts drinnen sind damit das nicht statisch ist
    private int countRoom = 0;
    private int countKitchen = 0;

    //Flag um zu erkennen ob Flur die active location ist oder nicht (da wir ja keine cloudlocation davon haben)
    private boolean flurActive = false;

    public BeaconService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        cloudManager = new IndoorCloudManagerFactory().create(this,cloudCredentials);
        allLocations = new HashMap<>();
        locationMap = new LocationMap();

        altBeaconManager = BeaconManager.getInstanceForApplication(this.getApplicationContext());
        altBeaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19"));

        Log.d("altbeaconRanging","Manager bound");

        cloudManager.getAllLocations(new CloudCallback<List<Location>>() {
            @Override
            public void success(List<Location> locations) {

                Location lastDefined = null;
                for (final Location location:locations) {

                    allLocations.put(location.getIdentifier(), location);

                    if(location.getIdentifier().equals("room-84l")){
                        Log.d("cloudService","added Location:" +location.getIdentifier());
                        lastDefined = location;
                    }

                    if(location.getIdentifier().equals("kitchen-2s1")){
                        Log.d("cloudService","added Location:" +location.getIdentifier());
                        lastDefined = location;
                    }

                    Log.d("cloudService","Got location: " + location.getIdentifier());

                }

                doneWithCloud(lastDefined);
            }

            @Override
            public void failure(EstimoteCloudException serverException) {
                Log.d("cloudService","Getting Location from Cloud failed: "+ serverException.toString());
            }
        });
    }

    public LocationMap getLocationMap(){
        return this.locationMap;
    }

    private void doneWithCloud(Location location){
        setCurrentLocation(location);

        Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_getLocation);
        broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM, location.getIdentifier());
        sendBroadcast(broadcast);

        //Ranging darf erst nach define location fertig ist beginnen
        altBeaconManager.bind(this);
    }

    /*private void defineLocations(){
        Log.d("defineLocations","called");
        LocationObject lastDefinedLocation = null;
        for (LocationObject location : allLocations) {
            HashMap<LocationObject, Door> neighboursList;
            Door bottomDoor = null;
            switch (location.getName()) {
                case "kitchen-2s1":
                    Log.d("defineLocations",location.getName());
                    location.setHeight(5.0);
                    location.setWidth(3.5);
                    location.setStartPointX(2.0);
                    location.setStartPointY(0.75);
                    bottomDoor = new Door("bottom", 2.5, 0, 3.0, 0);
                    neighboursList = new HashMap<>();
                    neighboursList.put(locationMap.getLocationByName("room-84l"), bottomDoor);
                    location.setNeighboursList(neighboursList);
                    lastDefinedLocation = location;
                    break;
                case "room-84l":
                    Log.d("defineLocations",location.getName());
                    location.setHeight(6.0);
                    location.setWidth(5.0);
                    location.setStartPointX(0.5);
                    location.setStartPointY(7.8);
                    Door topDoor = new Door("top", 1.0, 5.0, 1.5, 5.0);
                    neighboursList = new HashMap<>();
                    neighboursList.put(locationMap.getLocationByName("kitchen-2s1"), topDoor);
                    location.setNeighboursList(neighboursList);
                    lastDefinedLocation = location;
                    Log.d("testing something", "Room: "+location.getName());
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

        Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_getLocation);
        broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM, "");
        sendBroadcast(broadcast);

        //Ranging darf erst nach define location fertig ist beginnen
        altBeaconManager.bind(this);
    }*/

    public void setCurrentLocation(Location newCurrent) {
        Log.d("altbeaconRanging",  "called new currentlocation "+newCurrent.getIdentifier());
        Log.d("locationManager",  "AAAAAAAAAAAcalled new currentlocation "+newCurrent.getIdentifier());
        activeLocation = newCurrent;
        //locationMap.setActiveLocation(newCurrent);

        indoorManagerInit(activeLocation);
    }

    private void indoorManagerInit(final Location locationToRange) {
        Log.d("locationManager","Init called");
        if (indoorManager != null){
            Log.d("locationManager", "Stopping Positioning for "+ indoorManager.toString().substring(30));
            indoorManager.stopPositioning();
        }
        indoorManager = new IndoorLocationManagerBuilder(BeaconService.this,locationToRange,cloudCredentials).withDefaultScanner().build();
        indoorManager.setOnPositionUpdateListener(new OnPositionUpdateListener() {
            @Override
            public void onPositionUpdate(LocationPosition position) {
                //TODO schauen ob es immer der selbe indoorManager ist der nur schneller updates schickt => ja ist immer das selbe objekt

                if(locationToRange.getIdentifier().equals(activeLocation.getIdentifier())){
                    Log.d("locationManager", "Mgr:"+ indoorManager.toString().substring(30) +" PosUpdate:" + position.getX() + ", " + position.getY()+" Location:"+locationToRange.getIdentifier() +
                            " Count:" + tempcount);
                    BeaconService.this.position = position;
                    Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_BeaconService);
                    broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM, position.getX() + "," +
                            position.getY()+","+locationToRange.getIdentifier());
                    sendBroadcast(broadcast);


                    BeaconService.this.setCount(BeaconService.this.tempcount+1);
                }else{
                    Log.d("locationManager","dropped PosUpdate for "+ locationToRange.getIdentifier());
                }
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
            Log.d("altbeaconRanging", "nearest Beacon:" + nearestBeacon.getId1());

            if (nearestBeacon.getId1().toString().startsWith("0xbbbb")){
                //countRoom += 1;
                if(!activeLocation.getIdentifier().equals("room-84l")|flurActive){
                    Log.d("altbeaconranging","changing Location to room");
                    setCurrentLocation(allLocations.get("room-84l"));
                    flurActive = false;
                }

                Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_BeaconService);
                broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM, "Beacon"+ " Room,"+nearestBeacon.getId2()+","+nearestBeacon.getDistance());
                sendBroadcast(broadcast);
            }else if(nearestBeacon.getId1().toString().startsWith("0xaaaa")){
                //countKitchen +=1;
                if(!activeLocation.getIdentifier().equals("kitchen-2s1")|flurActive){
                    Log.d("altbeaconranging","changing Location to kitchen");
                    setCurrentLocation(allLocations.get("kitchen-2s1"));
                    flurActive = false;
                }
                Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_BeaconService);
                broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM, "Beacon"+" Kitchen,"+nearestBeacon.getId2()+","+nearestBeacon.getDistance());
                sendBroadcast(broadcast);

            }else{
                //Flur beacon da sonst die position herumspringen würde
                Log.d("altbeaconranging","Location is now Flur");
                indoorManager.stopPositioning();
                flurActive = true;
                Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_BeaconService);
                broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM, "Beacon"+ " Flur,"+nearestBeacon.getId2()+","+nearestBeacon.getDistance());
                sendBroadcast(broadcast);

            }

          /*  if(countRoom >= 1){
                //Log.d("altbeaconranging", "current:"+locationMap.getActiveLocation().getName() + " vs:room-841 " + locationMap.getActiveLocation().getName().equals("room-84l"));
                if(!activeLocation.getIdentifier().equals("room-84l")){
                    Log.d("altbeaconranging","changing Location");
                    setCurrentLocation(allLocations.get("room-84l"));
                }
                countKitchen = 0;
                countRoom =0;

            }else if(countKitchen >= 1){
                if(!activeLocation.getIdentifier().equals("kitchen-2s1")){
                    Log.d("altbeaconranging","changing Location");
                    setCurrentLocation(allLocations.get("kitchen-2s1"));
                }
                countKitchen = 0;
                countRoom =0;
            }*/
        }
//        Log.d("altbeaconRanging","Current Loc:"+locationMap.getActiveLocation().getName()+ " CountKitchen: " + countKitchen+ " CountRoom:"+ countRoom);
    }

    public class BeaconBinder extends Binder {

        public BeaconService getService(){
            return BeaconService.this;
        }
    }

    public void setCount(int count) {
        this.tempcount = count;
    }
}