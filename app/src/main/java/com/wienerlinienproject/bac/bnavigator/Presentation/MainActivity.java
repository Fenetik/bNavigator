package  com.wienerlinienproject.bac.bnavigator.Presentation;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

import com.estimote.coresdk.common.config.EstimoteSDK;
import com.wienerlinienproject.bac.bnavigator.R;
import com.wienerlinienproject.bac.bnavigator.Service.BeaconService;
//import com.wienerlinienproject.bac.bnavigator.Service.CloudService;


public class MainActivity extends AppCompatActivity implements ServiceConnection {

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private BeaconService beaconService;
    //private CloudService cloudService;
    private ServiceCallbackReceiver callbackReceiver = new ServiceCallbackReceiver();
    boolean beaconServiceIsBound = false;
    private TextView beaconLog;

    //TODO in diesem Fall kann man nur eine Serviceconnection haben?

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //hier sollen nur sachen rein die die UI benötigt
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        beaconLog = (TextView) findViewById(R.id.beaconLog);
        beaconLog.setMovementMethod(new ScrollingMovementMethod());

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
    protected void onStart() {
        //hier sachen rein die unabhängig von der UI geladen werden können
        super.onStart();

        // AppId and AppToken von der developer.estimote-website
        EstimoteSDK.initialize(getApplicationContext(), "natasa-nikic-info-s-your-o-e6g", "b437d62cd736bece0f9a475fe861e3d4");

        Intent intentBeaconService = new Intent(this, BeaconService.class);
        bindService(intentBeaconService, this, Context.BIND_AUTO_CREATE);

        //TODO
        //Intent intentCloudService = new Intent (this, CloudService.class);
       //bindService(intentCloudService, this, Context.BIND_AUTO_CREATE);

        //filtern worauf der receiver hören soll (Anroid vrwendet genrel broadcast)
        IntentFilter filter = new IntentFilter();
        filter.addAction(ServiceCallbackReceiver.BROADCAST_BeaconService);
        filter.addAction(ServiceCallbackReceiver.BROADCAST_cloudService);
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


    public class ServiceCallbackReceiver extends BroadcastReceiver {

        public static final String BROADCAST_BeaconService ="com.wienerlinienproject.bac.bnavigator.beaconServiceAction";
        public static final String BROADCAST_cloudService ="com.wienerlinienproject.bac.bnavigator.cloudServiceAction";

        public static final String BROADCAST_PARAM = "param";

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("ServiceCallbackReceiver", "received callback");

            if(intent.getAction().equals(BROADCAST_BeaconService)){
                Log.d("ServiceCallbackReceiver", "got " + intent.getStringExtra(BROADCAST_PARAM));

                beaconLog.append("\n"+intent.getStringExtra(BROADCAST_PARAM));
            }
        }
    }

}
