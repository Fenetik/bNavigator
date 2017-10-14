package  com.wienerlinienproject.bac.bnavigator.Service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.estimote.coresdk.observation.region.beacon.BeaconRegion;
import com.estimote.coresdk.recognition.packets.Beacon;
import com.estimote.coresdk.service.BeaconManager;
import com.wienerlinienproject.bac.bnavigator.Presentation.MainActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeaconService extends Service {


    private  Map<String, List<String>> PLACES_BY_BEACONS;
    private BeaconManager beaconManager;
    private BeaconRegion region;
    private String destination = "Mc Donalds";

    private final IBinder mBinder = new BeaconBinder();

    public BeaconService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        beaconManager = new BeaconManager(getApplicationContext());

        region = new BeaconRegion("ranged region", null, null, null);

        Log.d("moniiii","setting Listener");
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

                    Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_BeaconService);
                    broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM, output);
                    sendBroadcast(broadcast);
                    Log.d("moni","sending msg:" +output);

                }else{
                    //dafuq
                }

                /*String s = "";
                for(Beacon temp : list){
                    s += "Beacons found:" + temp.getProximityUUID().toString() + "\n";
                }
                Log.d("monii","beacon discovered");
                String temp ="found one";

                Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_BeaconService);
                broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM, s);
                sendBroadcast(broadcast);
                Log.d("moni","sending msg:" +temp);*/
            }
        });

        Map<String, List<String>> placesByBeacons = new HashMap<>();
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

        placesByBeacons.put("4:4", new ArrayList<String>() {{
            add("Burgerking");
        }});

        placesByBeacons.put("1:1", new ArrayList<String>() {{
            add("Starbucks");
            add("Subway");
            add("Mc Donalds");
        }});
        PLACES_BY_BEACONS = Collections.unmodifiableMap(placesByBeacons);


    }

    private void run(){
        Log.d("BeaconService", "run");

        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override public void onServiceReady() {
                beaconManager.startRanging(region);
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

   public class BeaconBinder extends Binder {

       public BeaconService getService(){
            return BeaconService.this;
        }
    }

}
