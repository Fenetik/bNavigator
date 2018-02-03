package com.wienerlinienproject.bac.bnavigator.Service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.estimote.coresdk.cloud.api.CloudCallback;
import com.estimote.coresdk.common.exception.EstimoteCloudException;
import com.estimote.indoorsdk.algorithm.IndoorLocationManagerBuilder;
import com.estimote.indoorsdk.algorithm.OnPositionUpdateListener;
import com.estimote.indoorsdk.algorithm.ScanningIndoorLocationManager;
import com.estimote.indoorsdk.cloud.IndoorCloudManagerFactory;
import com.estimote.indoorsdk.cloud.Location;
import com.estimote.indoorsdk.cloud.IndoorCloudManager;
import com.estimote.indoorsdk.cloud.LocationPosition;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CloudService extends Service {

    //TODO Cloudservice lädt nur daten von der Cloud?
    //TODO wie arbeitet ein service mit einem anderen service? zusammenspiel cloudservice und beaconmanager bzw indoormanager?
    private IndoorCloudManager cloudManager;
    private ScanningIndoorLocationManager indoorManager;
    private Location location;

    private final IBinder mBinder = new CloudBinder();


    public CloudService() {}
        //TODO location speichern in der datenschicht

    @Override
    public void onCreate() {

        super.onCreate();

        cloudManager = new IndoorCloudManagerFactory().create(this);
        //TODO change indentifier
        //TODO Mehrere Locations laden, LocationMap erstellen(wie bekommt man die in die Activity?,Bruach ich das überhaupt)
        cloudManager.getLocation("my-kitchen", new CloudCallback<Location>() {
            @Override
            public void success(Location location) {
                // store the Location object for later,
                // you will need it to initialize the IndoorLocationManager!
                //
                // you can also pass it to IndoorLocationView to display a map:
                // indoorView = (IndoorLocationView) findViewById(R.id.indoor_view);
                // indoorView.setLocation(location);
                CloudService.this.location = location;
            }

            @Override
            public void failure(EstimoteCloudException serverException) {
                //TODO broadcast error
            }
        });
        indoorManager = new IndoorLocationManagerBuilder(this,location).withDefaultScanner().build();
        indoorManager.setOnPositionUpdateListener(new OnPositionUpdateListener() {
            @Override
            public void onPositionUpdate(LocationPosition position) {
                // here, we update the IndoorLocationView with the current position,
                // but you can use the position for anything you want
                //TODO do smth
            }

            @Override
            public void onPositionOutsideLocation() {
                //TODO do SMTH
            }
        });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        run();
        indoorManager.startPositioning();
        return mBinder;
    }


    private void run(){
        Log.d("CloudService", "run");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        //TODO Cloudmanager beenden
        indoorManager.stopPositioning();
        //Service wird sicher beendet sobald sich jemand unbindet
        this.stopSelf();

        return super.onUnbind(intent);
    }

    public class CloudBinder extends Binder {

        public CloudService getService(){
            return CloudService.this;
        }
    }
}
