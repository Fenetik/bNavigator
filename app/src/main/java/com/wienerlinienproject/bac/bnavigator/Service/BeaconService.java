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
import com.estimote.indoorsdk.view.IndoorLocationView;
import com.wienerlinienproject.bac.bnavigator.Presentation.MainActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeaconService extends Service {


    private  Map<String, List<String>> PLACES_BY_BEACONS;
    private String destination = "Mc Donalds";

    private BeaconManager beaconManager;
    private BeaconRegion region;

    private IndoorCloudManager cloudManager;
    private IndoorLocationView indoorView;
    private ScanningIndoorLocationManager indoorManager;
    private LocationPosition position;
    private Location location;

    private final IBinder mBinder = new BeaconBinder();

    public BeaconService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        beaconManager = new BeaconManager(getApplicationContext());
        region = new BeaconRegion("ranged region", null, null, null);

        Log.d("moniiii","setting Listener");
        /*beaconManager.setRangingListener(new BeaconManager.BeaconRangingListener() {
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

                    Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_BeaconService);
                    broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM, output);
                    sendBroadcast(broadcast);
                    Log.d("moni","sending msg:" +output);

                }else{
                    //dafuq
                }
            }
        });*/


        cloudManager = new IndoorCloudManagerFactory().create(this);
        cloudManager.getLocation("nats--room-p7q", new CloudCallback<Location>() {
            @Override
            public void success(Location location) {
                // store the Location object for later,
                // you will need it to initialize the IndoorLocationManager!
                //
                // you can also pass it to IndoorLocationView to display a map:

                BeaconService.this.location = location;
                Log.d("cloudService","Got location: " + location.getName());
                indoorManager = new IndoorLocationManagerBuilder(BeaconService.this,location).withDefaultScanner().build();
                indoorView.setLocation(location);
                indoorManager.setOnPositionUpdateListener(new OnPositionUpdateListener() {
                    @Override
                    public void onPositionUpdate(LocationPosition position) {
                        // here, we update the IndoorLocationView with the current position,
                        // but you can use the position for anything you want
                        Log.d("locationManager", "Got position: " + position.getX() + ", " + position.getY());
                        BeaconService.this.position = position;
                        Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_BeaconService);
                        broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM, position.getX() + "," + position.getY());
                        sendBroadcast(broadcast);
                        //indoorView.updatePosition(position);
                    }

                    @Override
                    public void onPositionOutsideLocation() {
                        BeaconService.this.position = null;
                        //indoorView.hidePosition();
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
        });

        /*Map<String, List<String>> placesByBeacons = new HashMap<>();
        placesByBeacons.put("3:2", new ArrayList<String>() {{
            //closest
            add("Mc Donalds");
            //second closest
            add("Starbucks");
            //furthest away
            add("Subway");
        }});
        placesByBeacons.put("2:2", new ArrayList<String>() {{
            add("Subway");
            add("Starbucks");
            add("Mc Donalds");
        }});

        placesByBeacons.put("1:2", new ArrayList<String>() {{
            add("Burgerking");
        }});

        placesByBeacons.put("2:1", new ArrayList<String>() {{
            add("Starbucks");
            add("Subway");
            add("Mc Donalds");
        }});
        PLACES_BY_BEACONS = Collections.unmodifiableMap(placesByBeacons);
*/

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

    public void setIndoorView(IndoorLocationView indoorView){

        this.indoorView = indoorView;
    }

    public LocationPosition getPosition(){ return position; }

    public class BeaconBinder extends Binder {

       public BeaconService getService(){
            return BeaconService.this;
        }
    }

}
