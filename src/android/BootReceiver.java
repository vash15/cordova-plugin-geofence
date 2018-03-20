package com.cowbell.cordova.geofence;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.List;


public class BootReceiver extends BroadcastReceiver {
    public static final String TAG = "GeofencePlugin";
    private Context context = null;
    private PendingIntent mGeofencePendingIntent;
    private List<Geofence> geofencesList;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.w(TAG, "Boot reciver");
        this.context = context;

        if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
            Log.w(TAG, "Start re-register...");
            GeoNotificationManager manager = new GeoNotificationManager(this.context);
            this.geofencesList = manager.loadFromStorageAndInitializeGeofences();

            GeofencingClient mGeofencingClient = LocationServices.getGeofencingClient(this.context);
            if ( this.geofencesList.size() > 0 ){
                Log.w(TAG, "I found "+this.geofencesList.size()+" geofences");
                mGeofencingClient.addGeofences( getGeofencingRequest(), getGeofencePendingIntent() );
            }
            
        }

    }

    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);


        if ( geofencesList.size() > 0 ){
            builder.addGeofences(geofencesList);
        }

        return builder.build();
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getGeofencePendingIntent() {
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this.context, GeofenceBroadcastReceiver.class);
        mGeofencePendingIntent = PendingIntent.getBroadcast(this.context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }

}
