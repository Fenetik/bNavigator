package  com.wienerlinienproject.bac.bnavigator.Service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
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
import com.estimote.internal_plugins_api.scanning.Beacon;
import com.estimote.internal_plugins_api.scanning.BluetoothScanner;
import com.wienerlinienproject.bac.bnavigator.Presentation.MainActivity;


import java.util.Collections;
import java.util.List;
import java.util.Map;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class BeaconService extends Service {


    private  Map<String, List<String>> PLACES_BY_BEACONS;
    private IndoorCloudManager cloudManager;
    private ScanningIndoorLocationManager indoorManager;
    private final CloudCredentials cloudCredentials = new EstimoteCloudCredentials("natasa-nikic-info-s-your-o-e6g",
                                                                      "b437d62cd736bece0f9a475fe861e3d4");


    private LocationPosition position;
    private Location location;
    private Location activeLocation;
    private Location locationFlur;
    private Location locationKitchen;
    private int tempcount = 1;

    private BluetoothScanner scanner;

    private final IBinder mBinder = new BeaconBinder();

    public BeaconService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();


        cloudManager = new IndoorCloudManagerFactory().create(this,cloudCredentials);
        cloudManager.getLocation("nats--kitchen-5h5", new CloudCallback<Location>() {
            @Override
            public void success(final Location location) {

                BeaconService.this.location = location;
                BeaconService.this.activeLocation = location;
                BeaconService.this.locationKitchen = location;

                Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_getLocation);
                broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM, "");
                sendBroadcast(broadcast);

                indoorManagerInit(locationKitchen);

                Log.d("cloudService","Got location: " + location.getName());
            }

            @Override
            public void failure(EstimoteCloudException serverException) {
                Log.d("cloudService","Getting Location from Cloud failed: "+ serverException.toString()+"nats--kitchen-5h5");
               /* Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_BeaconService);
                broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM,
                        "Getting Location from Cloud failed");
                sendBroadcast(broadcast);*/            }
        });


    }

    private void indoorManagerInit(final Location locationToRange) {

        indoorManager = new IndoorLocationManagerBuilder(BeaconService.this,locationToRange,cloudCredentials).withDefaultScanner().build();

        indoorManager.setOnPositionUpdateListener(new OnPositionUpdateListener() {

            @Override
            public void onPositionUpdate(LocationPosition position) {

                Log.d("locationManager", "Got position: " + position.getX() + ", " + position.getY()+" Location:"+locationToRange.getName() +
                " Count:" + tempcount);
                BeaconService.this.position = position;
                Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_BeaconService);
                broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM, position.getX() + "," +
                        position.getY()+","+locationToRange.getName());
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

    }

    @Override
    public IBinder onBind(Intent intent) {
        run();
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {

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

    public void setCount(int count) {
        this.tempcount = count;
    }
}
