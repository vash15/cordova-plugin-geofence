package com.cowbell.cordova.geofence;
import com.google.android.gms.location.Geofence;
import com.google.gson.annotations.Expose;

/**
 * Created by mgbelluco on 07/03/18.
 */

public class GeoNotification {

    @Expose public String id;
    @Expose public double latitude;
    @Expose public double longitude;
    @Expose public int radius;
    @Expose public int transitionType;


    public GeoNotification() {
    }

    public String toJson() {
        return Gson.get().toJson(this);
    }

    public Geofence toGeofence() {
        return new Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this geofence.
                .setRequestId( id )

                // Set the circular region of this geofence.
                .setCircularRegion(latitude, longitude, radius)

                // Set the expiration duration of the geofence. This geofence gets automatically
                // removed after this period of time.
                .setExpirationDuration( Geofence.NEVER_EXPIRE )

                // Set the transition types of interest. Alerts are only generated for these
                // transition. We track entry and exit transitions in this sample.
                .setTransitionTypes( transitionType ) //  Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT;

                // Create the geofence.
                .build();
    }

    public static GeoNotification fromJson(String json) {
        if (json == null) return null;
        return Gson.get().fromJson(json, GeoNotification.class);
    }

}
