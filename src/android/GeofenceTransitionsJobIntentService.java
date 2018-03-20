package com.cowbell.cordova.geofence;

/**
 * Created by Michele Belluco on 08/03/18.
 */

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.JobIntentService;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;


/**
 * Listener for geofence transition changes.
 *
 * Receives geofence transition events from Location Services in the form of an Intent containing
 * the transition type and geofence id(s) that triggered the transition. Creates a notification
 * as the output.
 */
public class GeofenceTransitionsJobIntentService extends JobIntentService {

    private static final String TAG = "GeofencePlugin";
    protected static final String GeofenceTransitionIntent = "com.cowbell.cordova.geofence.TRANSITION";
    private static final int JOB_ID = 573;



    /**
     * The list of geofences on local device
     */
    protected GeoNotificationStore store;

    /**
     * Convenience method for enqueuing work in to this service.
     */
    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, GeofenceTransitionsJobIntentService.class, JOB_ID, intent);
    }

    public GeofenceTransitionsJobIntentService(){
        store = new GeoNotificationStore(this);
    }

    /**
     * Handles incoming intents.
     * @param intent sent by Location Services. This Intent is provided to Location
     *               Services (inside a PendingIntent) when addGeofences() is called.
     */
    @Override
    protected void onHandleWork(Intent intent) {

        // Intent per il broadcast dell'evento
        Intent broadcastIntent = new Intent(GeofenceTransitionIntent);

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(this, geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            broadcastIntent.putExtra("error", errorMessage);
            sendBroadcast(broadcastIntent);
            return;
        }



        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            List<GeoNotification> geoNotifications = new ArrayList<GeoNotification>();

            // Mi scorro tutti i Geofence Triggerati
            for (Geofence fence : triggeringGeofences) {
                String fenceId = fence.getRequestId();
                GeoNotification geoNotification = store.getGeoNotification(fenceId);

                if (geoNotification != null) {
                    geoNotification.transitionType = geofenceTransition;
                    geoNotifications.add(geoNotification);
                }
            }

            // Avvio il trigger nel Javascript il broadcast
            if ( geoNotifications.size() > 0 ){
                broadcastIntent.putExtra("transitionData", Gson.get().toJson(geoNotifications));
                GeofencePlugin.onTransitionReceived(geoNotifications);

                // TODO: Inviare una notifica locale. Attualmente non ne abbiamo bisogno perchè si arrangia il server a farla
                /*
                Geofence geofence = triggeringGeofences.get(0);
                if ( geofence != null ){
                    GeoNotification geoNotification = store.getGeoNotification(geofence.getRequestId());

                    // Send notification and log the transition details.
                    Log.w(TAG, "Invio la notifica per "+notificationDetails);
                    SimpleNotification notification = new SimpleNotification(this);

                    int randomNum = ThreadLocalRandom.current().nextInt(1, 10000 + 1);
                    notification.show(randomNum, "Invio la notifica di "+getTransitionString(geofenceTransition)+" per "+ geoNotification.id );
                }
                */

            }else{
                String error = "No geofence founded on local storage.";
                Log.e(TAG, error);
                broadcastIntent.putExtra("error", error);
            }

        } else {
            String error = "Geofence transition error: " + geofenceTransition;
            Log.e(TAG, error);
            broadcastIntent.putExtra("error", error);
        }

        sendBroadcast(broadcastIntent);
    }


    /**
     * Maps geofence transition types to their human-readable equivalents.
     *
     * @param transitionType    A transition type constant defined in Geofence
     * @return                  A String indicating the type of transition

    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return "Entrata "; // getString(R.string.geofence_transition_entered);
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return "Uscita"; //getString(R.string.geofence_transition_exited);
            default:
                return "Unknow"; //getString(R.string.unknown_geofence_transition);
        }
    }
     */

}
