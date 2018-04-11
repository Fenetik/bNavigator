package  com.wienerlinienproject.bac.bnavigator.Presentation;

import android.Manifest;
import android.app.Activity;
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
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Arrays;

import com.estimote.indoorsdk_module.cloud.LocationPosition;
import com.estimote.indoorsdk_module.view.IndoorLocationView;
import com.wienerlinienproject.bac.bnavigator.R;
import com.wienerlinienproject.bac.bnavigator.Service.BeaconService;
//import com.wienerlinienproject.bac.bnavigator.Service.CloudService;


public class MainActivity extends AppCompatActivity implements ServiceConnection, PositionView.DestinationSetCallback {

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private BeaconService beaconService;
    //private CloudService cloudService;
    private ServiceCallbackReceiver callbackReceiver = new ServiceCallbackReceiver();
    boolean beaconServiceIsBound = false;
    private TextView beaconLog;
    private PositionView positionView;
   // private TouchImageView imageView;
    private IndoorLocationView indoorView;
    private boolean isFABOpen =false;

    private FloatingActionButton fabMenu;
    private FloatingActionButton fab1;
    private FloatingActionButton fab2;
    private FloatingActionButton fabMyLocation;


    private Toolbar myToolbar;


    //TODO in diesem Fall kann man nur eine Serviceconnection haben?

    //TODO: nochmal checken ob die Bluetooth-Standortfreigabe nicht zu spät erteilt wird - evtl. muss alles gefreezed werden, bis Bestätigung kommt

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //hier sollen nur sachen rein die die UI benötigt

        // Framelyout wäre gut für karte im hintergrund zeichen

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

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

        indoorView = (IndoorLocationView) findViewById(R.id.indoor_view);
        //positionView = (PositionView) findViewById(R.id.position);
        positionView = (PositionView) findViewById(R.id.map_view);
        //TODO Destination Icon laden
        positionView.setDestinationIcon(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_place_black_48dp));
        positionView.setBackgroundMap(ContextCompat.getDrawable(MainActivity.this, R.drawable.drawn_map));

        beaconLog = (TextView) findViewById(R.id.beaconLog);
        beaconLog.setMovementMethod(new ScrollingMovementMethod());

        myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        myToolbar.setTitle(Html.fromHtml("<font color='#ffffff'>Aspern</font>"));
        setSupportActionBar(myToolbar);

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
        fabMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isFABOpen){
                    showFABMenu();
                    fabMenu.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#777777")));
                }else{
                    fabMenu.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#b80004")));
                    closeFABMenu();
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


        //TODO
        //Intent intentCloudService = new Intent (this, CloudService.class);
        //bindService(intentCloudService, this, Context.BIND_AUTO_CREATE);

        //filtern worauf der receiver hören soll (Anroid vrwendet genrel broadcast)
        IntentFilter filter = new IntentFilter();
        filter.addAction(ServiceCallbackReceiver.BROADCAST_BeaconService);
        filter.addAction(ServiceCallbackReceiver.BROADCAST_getLocation);
        registerReceiver(callbackReceiver, filter);
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

    private void showFABMenu(){
        isFABOpen=true;
        fab1.animate().translationY(-getResources().getDimension(R.dimen.standard_55));
        fab2.animate().translationY(-getResources().getDimension(R.dimen.standard_105));
    }

    private void closeFABMenu(){
        isFABOpen=false;
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
        return true;
    }

    //TODO bei add destination die destination location raus holen
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.action_setMark:
                //TODO boolean machen, true returnen wenn position gesetzt wurde, erst dann visible setzen
                positionView.onSetPositionClicked();
                return true;

            case R.id.action_deleteMark:
                positionView.onDeletePostionClicked();
                myToolbar.getMenu().findItem(R.id.action_deleteMark).setVisible(false);
                myToolbar.getMenu().findItem(R.id.action_shareMark).setVisible(false);
                return true;

            case R.id.action_shareMark:
                Point dest = positionView.getDestinationPointer();
                if(dest == null){
                    Toast.makeText(MainActivity.this,"Please set destination first.",Toast.LENGTH_SHORT).show();
                    return true;
                }
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Share your destination");
                //TODO deep link
                builder.setPositiveButton(android.R.string.copy, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("ShareLocation", "Example Link");
                        clipboard.setPrimaryClip(clip);
                    }
                });
                builder.setMessage("Destination:" + dest.x + " " + dest.y);

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
        myToolbar.getMenu().findItem(R.id.action_deleteMark).setVisible(true);
        myToolbar.getMenu().findItem(R.id.action_shareMark).setVisible(true);

        positionView.resetListener();
    }

    public class ServiceCallbackReceiver extends BroadcastReceiver {

        public static final String BROADCAST_BeaconService ="com.wienerlinienproject.bac.bnavigator.beaconServiceAction";
        public static final String BROADCAST_getLocation ="com.wienerlinienproject.bac.bnavigator.cloudServiceGetLocation";

        public static final String BROADCAST_PARAM = "param";

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("MainActivity_Callback", "received callback");

            if(intent.getAction().equals(BROADCAST_BeaconService)){
                Log.d("MainActivity_Position", "got " + intent.getStringExtra(BROADCAST_PARAM));
                String positionStr = intent.getStringExtra(BROADCAST_PARAM);
                List<String> positionList = Arrays.asList(positionStr.split(","));
                double xPosLoc = Double.valueOf(positionList.get(0));
                double yPosLoc = Double.valueOf(positionList.get(1));
                double xPos = Double.valueOf(positionList.get(2));
                double yPos = Double.valueOf(positionList.get(3));
                String locationName = String.valueOf(positionList.get(4));
                //positionView.updatePosition(xPos, yPos,indoorView.getHeight(),indoorView.getWidth());
                //positionView.invalidate();

                //TODO Treshhold ab wann wirklich die Grafik neu gezeichnet werden soll!!!!
                positionView.updateUserPosition(xPosLoc, yPosLoc, xPos, yPos,indoorView.getHeight(),indoorView.getWidth(),
                        ContextCompat.getDrawable(MainActivity.this, R.drawable.drawn_map),locationName);

                indoorView.updatePosition(new LocationPosition(xPos, yPos, 0.0));
                beaconLog.append("x: " + positionView.getmPointerX() + " y: " + positionView.getmPointerY() + "\n");
                //beaconLog.append("x: " + positionView.getmPointerX() + " y: " + imageView.getmPointerY() + "\n");


            }else if(intent.getAction().equals(BROADCAST_getLocation)) {
//                indoorView.setLocation(beaconService.getLocation());
                Log.d("MainActivity_Location", "indoorview done" );
            }
        }
    }

}
