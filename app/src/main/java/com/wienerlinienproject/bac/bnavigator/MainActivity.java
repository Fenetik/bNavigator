package com.wienerlinienproject.bac.bnavigator;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.estimote.coresdk.common.config.EstimoteSDK;
import com.estimote.coresdk.observation.region.beacon.BeaconRegion;
import com.estimote.coresdk.recognition.packets.Beacon;
import com.estimote.coresdk.service.BeaconManager;
import com.wienerlinienproject.bac.bnavigator.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private BeaconManager mBeaconManager;
    private BeaconRegion region;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private String destination = "Mc Donalds";

    private static final Map<String, List<String>> PLACES_BY_BEACONS;

    static {
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

    @Override
    public void onResume() {
        Log.d("onresume","done");
        super.onResume();

        mBeaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override public void onServiceReady() {
                mBeaconManager.startRanging(region);
            }
        });
    }


    @Override
    protected void onDestroy() {
        Log.d("ondestroy","done");
        mBeaconManager.disconnect();
        super.onDestroy();
    }


    @Override
    public void onPause() {
        mBeaconManager.stopRanging(region);
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBeaconManager = new BeaconManager(getApplicationContext());
        region = new BeaconRegion("ranged region", null, null, null);

        // AppId and AppToken von der developer.estimote-website
        EstimoteSDK.initialize(getApplicationContext(), "natasa-nikic-info-s-your-o-e6g", "b437d62cd736bece0f9a475fe861e3d4");

        mBeaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override public void onServiceReady() {
                mBeaconManager.startMonitoring(region);
            }
        });

        mBeaconManager.setRangingListener(new BeaconManager.BeaconRangingListener() {
            @Override
            public void onBeaconsDiscovered(BeaconRegion region, List<Beacon> list) {
                if (!list.isEmpty()) {
                    Beacon nearestBeacon = list.get(0);
                    List<String> places = placesNearBeacon(nearestBeacon);
                    TextView temp =  ((TextView)MainActivity.this.findViewById(R.id.beaconLog));
                    temp.setMovementMethod(new ScrollingMovementMethod());
                    if(!places.isEmpty()){
                        //wenn nur 1 place in der Liste ist
                        if(places.size() == 1){
                            temp.append("\n"+places.get(0));
                            temp.append(" (Nothing to navigate.)");
                            Log.d("RangingListener", places.get(0));
                        }else{
                            temp.append("\n"+places.get(0));
                            temp.append(" "+navigate(nearestBeacon, destination));
                            Log.d("RangingListener", places.get(0));
                        }
                    }else {
                        Log.d("RangingListener", "No new places");
                    }
                }
            }
        });

        Log.d("oncreate","done");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access.");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener(){
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("BeaconInit", "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }

                return;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_ranging, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
