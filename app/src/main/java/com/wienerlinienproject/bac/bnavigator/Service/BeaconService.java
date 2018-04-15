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
import com.wienerlinienproject.bac.bnavigator.Presentation.MainActivity;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;

public class BeaconService extends Service implements BeaconConsumer,RangeNotifier{

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

    private Location activeLocation;
    private Location kitchenLocation;
    private Location roomLocation;

    public void setRoomCount(int roomCount) {
        this.roomCount = roomCount;
    }

    public void setKitchenCount(int kitchenCount) {
        this.kitchenCount = kitchenCount;
    }

    private int roomCount = 0;
    private int kitchenCount = 0;

    private LocationPosition position;
    private Region region;

    private final IBinder mBinder = new BeaconBinder();

    //altbeacon manager
    private BeaconManager altBeaconManager;

    public BeaconService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();


        cloudManager = new IndoorCloudManagerFactory().create(this,cloudCredentials);


        altBeaconManager = BeaconManager.getInstanceForApplication(this.getApplicationContext());
        altBeaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19"));
        altBeaconManager.bind(this);
        Log.d("altbeaconRanging","Manager bound");

        cloudManager.getLocation("room-84l", new CloudCallback<Location>() {
            @Override
            public void success(final Location location) {

                BeaconService.this.activeLocation = location;
                BeaconService.this.roomLocation = location;

                Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_getLocation);
                broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM, location.getIdentifier());
                sendBroadcast(broadcast);

                Log.d("cloudService", "got location");
            }

            @Override
            public void failure(EstimoteCloudException serverException) {
                Log.d("cloudService","Getting Location from Cloud failed: "+ serverException.toString()+"room-84l");

            }
        });

        cloudManager.getLocation("kitchen-2s1", new CloudCallback<Location>() {
            @Override
            public void success(final Location location) {

                BeaconService.this.activeLocation = location;
                BeaconService.this.kitchenLocation = location;

                Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_getLocation);
                broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM, location.getIdentifier());
                sendBroadcast(broadcast);

                Log.d("cloudService", "got location");
            }

            @Override
            public void failure(EstimoteCloudException serverException) {
                Log.d("cloudService","Getting Location from Cloud failed: "+ serverException.toString()+"kitchen-2s1");

            }
        });

    }


    public void setActiveLocation(Location newCurrent) {
        Log.d("currentLoc",  "called new currentlocation "+newCurrent.getIdentifier());
        this.activeLocation = newCurrent;
        indoorManagerInit(activeLocation);
    }

    private void indoorManagerInit(final Location locationToRange) {

        indoorManager = new IndoorLocationManagerBuilder(BeaconService.this,locationToRange,cloudCredentials).withPositionUpdateInterval(501).withDefaultScanner().build();
        indoorManager.setOnPositionUpdateListener(new OnPositionUpdateListener() {

            @Override
            public void onPositionUpdate(LocationPosition position) {

                Log.d("locationManager", "Got position: " + position.getX() + ", " + position.getY()+" Location:"+locationToRange.getIdentifier());
                BeaconService.this.position = position;
                Intent broadcast = new Intent(MainActivity.ServiceCallbackReceiver.BROADCAST_BeaconService);
                broadcast.putExtra(MainActivity.ServiceCallbackReceiver.BROADCAST_PARAM, position.getX() + "," +
                        position.getY()+","+locationToRange.getIdentifier());
                sendBroadcast(broadcast);
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
        try {
            altBeaconManager.stopRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        altBeaconManager.unbind(this);
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


        } catch (RemoteException e) {
            e.printStackTrace();
        }
        altBeaconManager.setRangeNotifier(this);
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> collection, Region region) {
        Log.d("altbeaconRangin", "didRangeBeaconsInReagion " + collection.size());
        Beacon nearestBeacon = null;
        for (Beacon beacon : collection) {
            if (nearestBeacon == null)
                nearestBeacon = beacon;
            if (beacon.getDistance() < nearestBeacon.getDistance())
                nearestBeacon = beacon;
        }
        Identifier namespace = nearestBeacon.getId1();
        if (namespace.toString().startsWith("bbbb")) {
            setRoomCount(roomCount + 1);
        }
        if (namespace.toString().startsWith("aaaa")) {
            setKitchenCount(kitchenCount + 1);
        }
        if(roomCount > 3){
            setRoomCount(0);
            setKitchenCount(0);
            Log.d("altbeaconRangin", "roomCount > 3");
            if(!roomLocation.getIdentifier().equals(activeLocation.getIdentifier()))
                setActiveLocation(roomLocation);
        } else if(kitchenCount > 3){
            setKitchenCount(0);
            setRoomCount(0);
            Log.d("altbeaconRangin", "kitchenCount > 3");
            if(!kitchenLocation.getIdentifier().equals(activeLocation.getIdentifier()))
                setActiveLocation(kitchenLocation);
        }
    }

    public class BeaconBinder extends Binder {

       public BeaconService getService(){
            return BeaconService.this;
        }
    }
}
