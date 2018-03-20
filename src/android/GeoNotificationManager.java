package com.cowbell.cordova.geofence;

import android.content.Context;
import com.google.android.gms.location.Geofence;
import org.apache.cordova.CallbackContext;

import java.util.ArrayList;
import java.util.List;

public class GeoNotificationManager {
    private GeoNotificationStore geoNotificationStore;
    private List<Geofence> geoFences;



    public GeoNotificationManager(Context context) {
        geoNotificationStore = new GeoNotificationStore(context);
    }

    public List<Geofence> loadFromStorageAndInitializeGeofences() {
        List<GeoNotification> geoNotifications = geoNotificationStore.getAll();
        geoFences = new ArrayList<Geofence>();
        for (GeoNotification geo : geoNotifications) {
            geoFences.add( geo.toGeofence() );
        }
        return geoFences;
    }

    public List<GeoNotification> getWatched() {
        List<GeoNotification> geoNotifications = geoNotificationStore.getAll();
        return geoNotifications;
    }


    public void addGeoNotifications(List<GeoNotification> geoNotifications) {
        for (GeoNotification geo : geoNotifications) {
            geoNotificationStore.setGeoNotification(geo);
        }
    }

    public void removeGeoNotifications(List<String> ids) {
        for (String id : ids) {
            geoNotificationStore.remove(id);
        }
    }

    public void removeAllGeoNotifications() {
        List<GeoNotification> geoNotifications = geoNotificationStore.getAll();
        List<String> geoNotificationsIds = new ArrayList<String>();
        for (GeoNotification geo : geoNotifications) {
            geoNotificationsIds.add(geo.id);
        }
        removeGeoNotifications(geoNotificationsIds);
    }

}
