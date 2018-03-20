package com.cowbell.cordova.geofence;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.Manifest;
import android.app.Activity;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.util.ArrayList;
import java.util.List;


public class GeofencePlugin extends CordovaPlugin  { // implements OnCompleteListener<Void>


    public static final String           TAG      = "GeofencePlugin";
    public static       CordovaWebView   webView  = null;
    public static       CordovaInterface aCordova = null;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    /**
     * Tracks whether the user requested to add or remove geofences, or to do neither.
     */
    private enum PendingGeofenceTask {
        ADD, REMOVE, NONE
    }

    private PendingGeofenceTask mPendingGeofenceTask = PendingGeofenceTask.NONE;

    /**
     * Provides access to the Geofencing API.
     */
    private GeofencingClient mGeofencingClient;

    /**
     * Used when requesting to add or remove geofences.
     */
    private PendingIntent mGeofencePendingIntent;

    /**
     * The list of geofences used in this sample.
     */
    private ArrayList<Geofence> mGeofenceList;

    /**
     * The list of geofences on local device
     */
    private GeoNotificationManager geoNotificationManager;


    private Context context = null;
    private class Action {
        public String action;
        public JSONArray args;
        public CallbackContext callbackContext;

        public Action(String action, JSONArray args, CallbackContext callbackContext) {
            this.action = action;
            this.args = args;
            this.callbackContext = callbackContext;
        }
    }

    //FIXME: what about many executedActions at once
    private Action executedAction;



    /**
     * @param cordova
     *            The context of the main Activity.
     * @param webView
     *            The associated CordovaWebView.
     */
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        GeofencePlugin.webView  = webView;
        GeofencePlugin.aCordova = cordova;

        this.context                = this.cordova.getActivity().getApplicationContext();
        this.mGeofenceList          = new ArrayList<>();
        this.mGeofencingClient      = LocationServices.getGeofencingClient(this.cordova.getActivity());
        this.geoNotificationManager = new GeoNotificationManager(context);
    }

    @Override
    public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {

        Log.d(TAG, "GeofencePlugin execute action: " + action + " args: " + args.toString());

        this.executedAction = new Action(action, args, callbackContext);

        this.cordova.getThreadPool().execute(new Runnable() {
            public void run() {

                if ( action.equals("initialize") || action.equals("requestPermissions") ) {
                    if ( !checkPermissions() ) {
                        requestPermissions();
                    }
                    callbackContext.success();
                } else if (action.equals("addOrUpdate")) {

                    addOrUpdate(args);

                } else if (action.equals("remove")) {

                    removeGeofences(args);

                } else if (action.equals("removeAll")) {

                    removeAllGeofences();

                } else if (action.equals("getWatched")) {

                    getWatched();

                } else if (action.equals("deviceReady")) {
                    deviceReady();
                }
            }
        });

        return true;
    }

    public boolean execute(Action action) throws JSONException {
        return execute(action.action, action.args, action.callbackContext);
    }

    private GeoNotification parseFromJSONObject(JSONObject object) {
        GeoNotification geo = GeoNotification.fromJson(object.toString());
        return geo;
    }

    // Controlla se ho i permessi
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this.context,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    // Richiede i permessi
    private void requestPermissions() {
        // boolean shouldProvideRationale =  ActivityCompat.shouldShowRequestPermissionRationale(this.cordova.getActivity(), Manifest.permission.ACCESS_FINE_LOCATION);
        this.cordova.requestPermission(this, REQUEST_PERMISSIONS_REQUEST_CODE, Manifest.permission.ACCESS_FINE_LOCATION ); // new String[]{Manifest.permission.ACCESS_FINE_LOCATION}
    }


    // Richiama una callback javascript dell'evento intercettato
    public static void onTransitionReceived(List<GeoNotification> notifications) {
        Log.d(TAG, "Transition Event Received!");
        sendJavascript("setTimeout('geofence.onTransitionReceived(" + Gson.get().toJson(notifications) + ")',0)");
    }


    private void deviceReady() {
        Intent intent = cordova.getActivity().getIntent();
        String data = intent.getStringExtra("geofence.notification.data");
        String js = "javascript: setTimeout('geofence.onNotificationClicked(" + data + ")',0);";

        if (data == null) {
            Log.d(TAG, "No notifications clicked.");
        } else {
            sendJavascript(js);
        }

    }

    private void getWatched(){
        final GeofencePlugin self = this;
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                List<GeoNotification> geoNotifications = self.geoNotificationManager.getWatched();
                self.executedAction.callbackContext.success(Gson.get().toJson(geoNotifications));
            }
        });
    }


    // Invia del javascript alla webview
    static void sendJavascript(String script){
        if (webView == null || aCordova == null) {
            Log.d(TAG, "Webview  or cordova not initialized");
            return;
        }
        aCordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ( script.length() > 0 ){
                    if (script.startsWith("javascript:") ){
                        webView.loadUrl(script);
                    }else{
                        webView.loadUrl("javascript:"+script);
                    }
                }
            }
        });
    }

    // Aggiunge o modifica il Geofence
    private void addOrUpdate(final JSONArray args){

        final GeofencePlugin self = this;

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                if (!checkPermissions()) {
                    self.executedAction.callbackContext.error("Insufficient permissions");
                    return;
                }

                List<GeoNotification> geoNotifications = new ArrayList<GeoNotification>();
                for (int i = 0; i < args.length(); i++) {
                    GeoNotification geo = parseFromJSONObject(args.optJSONObject(i));
                    if (geo != null) {
                        self.mGeofenceList.add( geo.toGeofence() );
                        geoNotifications.add(geo);
                    }
                }

                // Salvo in locale le Geofence
                self.geoNotificationManager.addGeoNotifications(geoNotifications);

                // Registro le Geofence
                self.mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                    .addOnCompleteListener(self.cordova.getActivity(), new OnCompleteListener<Void>() {
                        /**
                         * Runs when the result of calling {@link #addGeofences()} and/or {@link #removeGeofences()}
                         * is available.
                         * @param task the resulting Task, containing either a result or error.
                         */
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Log.w(TAG, "Aggiunta completata");
                            if (task.isSuccessful()) {
                                self.executedAction.callbackContext.success();
                            } else {
                                // Get the status code for the error and log it using a user-friendly message.
                                String errorMessage = GeofenceErrorMessages.getErrorString(self.context, task.getException());
                                Log.e(TAG, errorMessage);
                                self.executedAction.callbackContext.error(errorMessage);
                            }

                        }
                    });

            }
        });
    }

    private void removeGeofences(final JSONArray args){
        final GeofencePlugin self = this;
        Activity activity = this.cordova.getActivity();

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                List<String> ids = new ArrayList<String>();
                for (int i = 0; i < args.length(); i++) {
                    ids.add(args.optString(i));
                }
                self.geoNotificationManager.removeGeoNotifications(ids);

                self.mGeofencingClient.removeGeofences(ids)
                    .addOnSuccessListener(activity, new OnSuccessListener<Void>() {
                        @Override
                            public void onSuccess(Void aVoid) {
                                self.executedAction.callbackContext.success();
                            }
                        })
                    .addOnFailureListener(activity, new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                self.executedAction.callbackContext.error(GeofenceErrorMessages.getErrorString(self.context, e));
                            }
                        });
            }

        });
    }

    // Rimuovi tutti i Geofence
    private void removeAllGeofences(){
        final GeofencePlugin self = this;
        Activity activity = this.cordova.getActivity();

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {

                // Cancella tutti i Geofence dal LocalStorage
                self.geoNotificationManager.removeAllGeoNotifications();

                // Rimuove tutti i Geofence.
                self.mGeofencingClient.removeGeofences(getGeofencePendingIntent())
                        .addOnSuccessListener(activity, new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                self.executedAction.callbackContext.success();
                            }
                        })
                        .addOnFailureListener(activity, new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                self.executedAction.callbackContext.error(GeofenceErrorMessages.getErrorString(self.context, e));
                            }
                        });

            }

        });
    }

    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    @NonNull
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(mGeofenceList);

        // Return a GeofencingRequest.
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
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }

        Intent intent = new Intent(this.cordova.getActivity(), GeofenceBroadcastReceiver.class);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        mGeofencePendingIntent = PendingIntent.getBroadcast(this.context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }



}
