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
import com.wienerlinienproject.bac.bnavigator.Presentation.MainActivity;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BeaconService extends Service {


    private  Map<String, List<String>> PLACES_BY_BEACONS;
    private String destination = "Mc Donalds";

    private BeaconManager beaconManager;
    private BeaconRegion region;

    private IndoorCloudManager cloudManager;
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

        //TODO alle locations laden
        cloudManager = new IndoorCloudManagerFactory().create(this);
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
        });
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

    public class BeaconBinder extends Binder {

       public BeaconService getService(){
            return BeaconService.this;
        }
    }

    public Location getLocation() {
        return location;
    }
}
