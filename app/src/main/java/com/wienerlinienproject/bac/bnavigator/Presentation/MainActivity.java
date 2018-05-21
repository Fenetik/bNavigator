package com.wienerlinienproject.bac.bnavigator.Presentation;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Point;
import android.net.UrlQuerySanitizer;
import android.os.Build;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;


import com.estimote.indoorsdk_module.view.IndoorLocationView;
import com.wienerlinienproject.bac.bnavigator.Data.Door;
import com.wienerlinienproject.bac.bnavigator.Data.LocationMap;
import com.wienerlinienproject.bac.bnavigator.Data.LocationObject;
import com.wienerlinienproject.bac.bnavigator.R;
import com.wienerlinienproject.bac.bnavigator.Service.BeaconService;


public class MainActivity extends AppCompatActivity implements ServiceConnection, PositionView.DestinationSetCallback {

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private BeaconService beaconService;
    private ServiceCallbackReceiver callbackReceiver = new ServiceCallbackReceiver();
    boolean beaconServiceIsBound = false;
    private TextView locationLog;
    private TextView beaconLog;
    private PositionView positionView;
  //  private IndoorLocationView indoorView;
    private boolean isFABOpen = false;

    MenuItem deleteMark;
    MenuItem shareMark;

    private FloatingActionButton fabMenu;
    private FloatingActionButton fab1;
    private FloatingActionButton fab2;
    private FloatingActionButton fabMyLocation;
    private FloatingActionButton fabNavigateMe;

    private Button debugModeBtn;
    private boolean debugMode = false;
    private boolean arrivedAtDestination;

    private LocationMap locationMap;
    private int counter;

    private Toolbar myToolbar;
    private boolean myToolbarIsInflated = false;
    private boolean myToolBarDestinationSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //hier sollen nur sachen rein die die UI benötigt

        // Framelyout wäre gut für karte im hintergrund zeichen

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access.");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }

        counter = 0;

        arrivedAtDestination = false;
        //positionView = (PositionView) findViewById(R.id.position);
        positionView = (PositionView) findViewById(R.id.map_view);

        //indoorView = (IndoorLocationView) findViewById(R.id.indoor_view);

        myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        myToolbar.setTitle(Html.fromHtml("<font color='#ffffff'>Aspern</font>"));
        setSupportActionBar(myToolbar);

        //deleteMark = myToolbar.getMenu().findItem(R.id.action_deleteMark);
        //shareMark = myToolbar.getMenu().findItem(R.id.action_shareMark);

        positionView.setDestinationIcon(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_place_black_48dp));
        positionView.setBackgroundMap(ContextCompat.getDrawable(MainActivity.this, R.drawable.drawn_map));

        locationLog = (TextView) findViewById(R.id.locationLog);
        locationLog.setMovementMethod(new ScrollingMovementMethod());

        beaconLog = (TextView) findViewById(R.id.beaconLog);
        beaconLog.setMovementMethod(new ScrollingMovementMethod());


        fabMyLocation = (FloatingActionButton) findViewById(R.id.fabLocation);
        fabMyLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                positionView.scrollToUser();
            }
        });

        fabMenu = (FloatingActionButton) findViewById(R.id.fabMenu);
        fab1 = (FloatingActionButton) findViewById(R.id.fabFloor1);
        fab2 = (FloatingActionButton) findViewById(R.id.fabFloor2);
        fabNavigateMe = (FloatingActionButton) findViewById(R.id.fabNavigateMe);

        debugModeBtn = (Button) findViewById(R.id.BtnDebug);

        fabNavigateMe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                arrivedAtDestination = positionView.navigateUser();
            }
        });

        fabMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isFABOpen) {
                    showFABMenu();
                    fabMenu.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#777777")));
                } else {
                    fabMenu.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#b80004")));
                    closeFABMenu();
                }
            }
        });

        debugModeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!debugMode) {
                    locationLog.setVisibility(View.VISIBLE);
                    beaconLog.setVisibility(View.VISIBLE);
                    debugMode = true;
                } else {
                    locationLog.setVisibility(View.INVISIBLE);
                    beaconLog.setVisibility(View.INVISIBLE);
                    debugMode = false;
                }
            }
        });

    }

    @Override
    protected void onStart() {
        //hier sachen rein die unabhängig von der UI geladen werden können
        super.onStart();

        Intent intentBeaconService = new Intent(this, BeaconService.class);
        bindService(intentBeaconService, this, Context.BIND_AUTO_CREATE);

        //filtern worauf der receiver hören soll (Anroid vrwendet genrel broadcast)
        IntentFilter filter = new IntentFilter();
        filter.addAction(ServiceCallbackReceiver.BROADCAST_BeaconService);
        filter.addAction(ServiceCallbackReceiver.BROADCAST_getLocation);
        registerReceiver(callbackReceiver, filter);

        fillLocationMap();
        positionView.setLocationMap(locationMap);

        if (getIntent().getDataString() != null) {
            UrlQuerySanitizer sanitizer = new UrlQuerySanitizer(getIntent().getDataString());
            Log.e("IntentShare", sanitizer.getValue("x"));
            Point p = new Point(Integer.parseInt(sanitizer.getValue("x")),Integer.parseInt(sanitizer.getValue("y")));
            positionView.setDestination(p,sanitizer.getValue("loc"));
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        BeaconService.BeaconBinder binder = (BeaconService.BeaconBinder) iBinder;
        beaconService = binder.getService();
        beaconServiceIsBound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        beaconService = null;
        beaconServiceIsBound = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
        unregisterReceiver(callbackReceiver);
    }

    private void showFABMenu() {
        isFABOpen = true;
        fab1.animate().translationY(-getResources().getDimension(R.dimen.standard_55));
        fab2.animate().translationY(-getResources().getDimension(R.dimen.standard_105));
    }

    private void closeFABMenu() {
        isFABOpen = false;
        fab1.animate().translationY(0);
        fab2.animate().translationY(0);
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
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        myToolbarIsInflated = true;
        shareMark = myToolbar.getMenu().findItem(R.id.action_shareMark);
        deleteMark = myToolbar.getMenu().findItem(R.id.action_deleteMark);

        if(myToolBarDestinationSet){
            deleteMark.setVisible(true);
            shareMark.setVisible(true);
            fabNavigateMe.setVisibility(View.VISIBLE);
        }

        return true;
    }

    public void showArrivedAtDestination() {
        Log.d("Mainactivity", "showArrivedAtDestination called");
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Arrived at your destination");
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Arrived", "You have arrived at the destination");
                clipboard.setPrimaryClip(clip);
            }
        });
        builder.setMessage("Have fun with your friends!");

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.show();
        positionView.setArrivedAtDestination(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_setMark:
                positionView.onSetPositionClicked();
                return true;

            case R.id.action_deleteMark:
                positionView.onDeletePositionClicked();
                myToolbar.getMenu().findItem(R.id.action_deleteMark).setVisible(false);
                myToolbar.getMenu().findItem(R.id.action_shareMark).setVisible(false);
                fabNavigateMe.setVisibility(View.INVISIBLE);
                return true;

            case R.id.action_shareMark:
                final Point dest = positionView.getDestinationPointer();
                final String destinationLocation = positionView.getDestinationLocationObject().getName();
                if (dest == null) {
                    Toast.makeText(MainActivity.this, "Please set destination first.", Toast.LENGTH_SHORT).show();
                    return true;
                }
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Share your destination");
                builder.setPositiveButton(android.R.string.copy, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("ShareLocation",
                                "Let´s meet here: https://www.bnavigator.at/share?loc="+destinationLocation+"&x="+dest.x+"&y="+dest.y);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(MainActivity.this, "Copied to clipboard.", Toast.LENGTH_SHORT).show();
                    }
                });
                builder.setMessage("Destination:" +destinationLocation+" "+ dest.x + " " + dest.y);

                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                    }
                });
                builder.show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestinationSet() {
        if(myToolbarIsInflated){
            deleteMark.setVisible(true);
            shareMark.setVisible(true);
            fabNavigateMe.setVisibility(View.VISIBLE);

        }else{
            myToolBarDestinationSet = true;
        }
        //myToolbar.getMenu().findItem(R.id.action_deleteMark).setVisible(true);
       //shareMark.setVisible(true);

        positionView.resetListener();
    }

    @Override
    public void onTargetReached() {
        deleteMark.setVisible(false);
        shareMark.setVisible(false);
        fabNavigateMe.setVisibility(View.INVISIBLE);
        showArrivedAtDestination();

    }

    public class ServiceCallbackReceiver extends BroadcastReceiver {

        public static final String BROADCAST_BeaconService = "com.wienerlinienproject.bac.bnavigator.beaconServiceAction";
        public static final String BROADCAST_getLocation = "com.wienerlinienproject.bac.bnavigator.cloudServiceGetLocation";
        public static final String BROADCAST_nearestBeacon = "com.wienerlinienproject.bac.bnavigator.beaconServiceNearestBeacon";


        public static final String BROADCAST_PARAM = "param";

        @Override
        public void onReceive(Context context, Intent intent) {
            arrivedAtDestination = positionView.getArrivedAtDestination();

            if (intent.getAction().equals(BROADCAST_BeaconService)) {
                if (!intent.getStringExtra(BROADCAST_PARAM).startsWith("Beacon")) {
                    Log.d("MainActivity_Position", "got " + intent.getStringExtra(BROADCAST_PARAM));
                    String positionStr = intent.getStringExtra(BROADCAST_PARAM);
                    List<String> positionList = Arrays.asList(positionStr.split(","));

                    double xPos = Double.valueOf(positionList.get(0));
                    double yPos = Double.valueOf(positionList.get(1));
                    String locationName = String.valueOf(positionList.get(2));

                    updateActiveLocation(locationName);

                    DecimalFormat df = new DecimalFormat("#.####");

                    positionView.updateUserPosition(xPos, yPos, positionView.getHeight(), positionView.getWidth(),
                            ContextCompat.getDrawable(MainActivity.this, R.drawable.drawn_map));

                    Spannable wordtoSpan;
                    if (locationName.equals("kitchen-2s1")) {
                        wordtoSpan = new SpannableString("\nKitchen: x: " + df.format(positionView.getRelativeUserPosX()) + " y: " + df.format(positionView.getRelativeUserPosY()));
                        wordtoSpan.setSpan(new ForegroundColorSpan(Color.parseColor("#FFF000")), 0, 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else if (locationName.equals("room-84l")) {
                        wordtoSpan = new SpannableString("\nRoom: x: " + df.format(positionView.getRelativeUserPosX()) + " y: " + df.format(positionView.getRelativeUserPosY()));
                        wordtoSpan.setSpan(new ForegroundColorSpan(Color.parseColor("#000FFF")), 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else {
                        wordtoSpan = new SpannableString("\nFlur: x: " + df.format(positionView.getRelativeUserPosX()) + " y: " + df.format(positionView.getRelativeUserPosY()));
                        wordtoSpan.setSpan(new ForegroundColorSpan(Color.parseColor("#FF0000")), 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    locationLog.append(wordtoSpan);

                    //locationLog.append(locationName+": x: " + positionView.getmPointerX() + " y: " + positionView.getmPointerY() +"\n");
                } else {
                    String positionStr = intent.getStringExtra(BROADCAST_PARAM);
                    String[] beaconInfo = positionStr.split(",");
                    DecimalFormat df = new DecimalFormat("#.##");
                    String locationNearestBeacon = beaconInfo[0].substring(7, beaconInfo[0].length());
                    if (locationNearestBeacon.equals("Flur")) {
                        locationMap.setActiveLocation(locationMap.getLocationByName("flur"));
                        positionView.updateUserPosition(0.5, 1.0, positionView.getHeight(), positionView.getWidth(),
                                ContextCompat.getDrawable(MainActivity.this, R.drawable.drawn_map));
                        beaconLog.append("\nFlur:" + beaconInfo[1].substring(0, 4) + " " + beaconInfo[2].substring(0, 5) + "m");
                    } else if (locationNearestBeacon.equals("Kitchen")) {
                        beaconLog.append("\nKitchen:" + beaconInfo[1].substring(0, 4) + " " + beaconInfo[2].substring(0, 5) + "m");
                    } else {
                        beaconLog.append("\nRoom:" + beaconInfo[1].substring(0, 4) + " " + beaconInfo[2].substring(0, 5) + "m");
                    }
                }
            } else if (intent.getAction().equals(BROADCAST_getLocation)) {
                //fillLocationMap(intent.getStringExtra(BROADCAST_PARAM));
                //locationMap.setActiveLocation(locationMap.getLocationByName(lastDefinedLocation));
                locationMap.setActiveLocation(locationMap.getLocationByName(intent.getStringExtra(BROADCAST_PARAM)));

                Log.d("MainActivity_Location", "indoorview done");
            }
        }
    }

    private void updateActiveLocation(String name) {
        locationMap.setActiveLocation(locationMap.getLocationByName(name));
    }

    private void fillLocationMap() {
        locationMap = new LocationMap();
        HashMap<LocationObject, Door> neighboursListKitchen = new HashMap<>();
        HashMap<LocationObject, Door> neighboursListRoom = new HashMap<>();
        HashMap<LocationObject, Door> neighboursListFlur = new HashMap<>();
        LocationObject kitchen = new LocationObject("kitchen-2s1");
        kitchen.setHeight(5.0);
        kitchen.setWidth(3.5);
        kitchen.setStartPointX(2.0);
        kitchen.setStartPointY(0.75);
        Door bottomDoor = new Door("bottom", 2.5, 0, 3.0, 0);
        Door bottomDoorKitchen = new Door("bottom", 0.5, 0, 0.75, 0);

        LocationObject room = new LocationObject("room-84l");
        room.setHeight(6.0);
        room.setWidth(5.0);
        room.setStartPointX(0.5);
        room.setStartPointY(7.8);

        LocationObject flur = new LocationObject("flur");
        flur.setHeight(2.0);
        flur.setWidth(1.0);
        flur.setStartPointX(2.5);
        flur.setStartPointY(5.75);
        Door bottomDoorFlur = new Door("bottom", 0.5, 0, 0.75, 0);
        Door topDoorFlur = new Door("top", 0.5, 2, 0.75, 2);

        locationMap.addLocation("kitchen-2s1", kitchen);
        locationMap.addLocation("room-84l", room);
        locationMap.addLocation("flur", flur);

        Door topDoor = new Door("top", 1.0, 5.0, 1.5, 5.0);
        Door topDoorRoom = new Door("top", 0.5, 2, 0.75, 2);
        neighboursListRoom.put(locationMap.getLocationByName("kitchen-2s1"), topDoor);
        room.setNeighboursList(neighboursListRoom);
        neighboursListKitchen.put(locationMap.getLocationByName("room-84l"), bottomDoor);
        neighboursListKitchen.put(locationMap.getLocationByName("flur"), bottomDoorKitchen);
        neighboursListRoom.put(locationMap.getLocationByName("flur"), topDoorRoom);
        kitchen.setNeighboursList(neighboursListKitchen);
        neighboursListFlur.put(locationMap.getLocationByName("kitchen-2s1"), topDoorFlur);
        neighboursListFlur.put(locationMap.getLocationByName("room-84l"), bottomDoorFlur);
        flur.setNeighboursList(neighboursListFlur);
        //locationMap.setActiveLocation(locationMap.getLocationByName(lastDefinedLocation));
    }
}

