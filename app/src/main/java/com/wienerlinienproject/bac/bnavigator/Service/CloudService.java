package com.wienerlinienproject.bac.bnavigator.Service;

import com.estimote.coresdk.cloud.api.CloudCallback;
import com.estimote.coresdk.common.exception.EstimoteCloudException;
import com.estimote.indoorsdk.cloud.Location;
import com.estimote.indoorsdk.cloud.IndoorCloudManager;

public class CloudService {

    private final IndoorCloudManager cloudManager;
    private Location location;

    public CloudService(IndoorCloudManager cloudManager) {
        this.cloudManager = cloudManager;
        //Location von der Cloud laden
        //TODO location name
        //TODO speichern in der datenschicht
        cloudManager.getLocation("my-kitchen", new CloudCallback<Location>() {
            @Override
            public void success(Location location) {
                // store the Location object for later,
                // you will need it to initialize the IndoorLocationManager!
                //
                // you can also pass it to IndoorLocationView to display a map:
                // indoorView = (IndoorLocationView) findViewById(R.id.indoor_view);
                // indoorView.setLocation(location);

                //MainActivit.this weil man ja in einem Callback drinnen ist
                CloudService.this.location = location;
            }

            @Override
            public void failure(EstimoteCloudException e) {
                //TODO message ausgeben dass cloud connection nicht erfolgreich war
                e.printStackTrace();
            }
        });
    }
}
