package edu.csulb.com.gostrike;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

import edu.csulb.com.gostrike.app.Extra;

import edu.csulb.com.gostrike.app.SoundEffects;

public class Dashboard extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, SensorEventListener, OnMapReadyCallback {

    //Define a request code to send to Google Play services
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private double currentLatitude;
    private double currentLongitude;

    public WebSocket webSocket2=null;

    GoogleMap googleMap;

    private SensorManager sManager;
    SensorEvent event;

    float[] mGravity;
    float[] mGeomagnetic;

    Marker marker;

    String username;
    TextView gamelog;

    String message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        //Initialize
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                // The next two lines tell the new client that “this” current class will handle connection stuff
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                //fourth line adds the LocationServices API endpoint from GooglePlayServices
                .addApi(LocationServices.API)
                .build();
        gamelog = (TextView) findViewById(R.id.gamelog);
        gamelog.setSelected(true);
        gamelog.setHorizontallyScrolling(true);

        message = "";

        SharedPreferences sf = getSharedPreferences("GoStrike",MODE_PRIVATE);
        username = sf.getString("username","Error");

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(500)        // 10 seconds, in milliseconds
                .setFastestInterval(200); // 1 second, in milliseconds

        //get a hook to the sensor service
        sManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        AsyncHttpClient.getDefaultInstance().websocket("ws://192.168.43.150:8080/join", "", new AsyncHttpClient.WebSocketConnectCallback() {
            @Override
            public void onCompleted(Exception ex, WebSocket webSocket) {
                if (ex != null) {
                    ex.printStackTrace();
                    return;
                }
                webSocket2 = webSocket;
                JSONObject jo = new JSONObject();
                try {
                    jo.put("event","new");
                    jo.put("id",username);
                    sendSocket(jo);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                webSocket2.setStringCallback(new WebSocket.StringCallback() {
                    public void onStringAvailable(String s) {
                        System.out.println("I got a string: " + s);
                        try {
                            JSONObject jo = new JSONObject(s);
                            if(jo.getString("Event").equals("kill"))
                            {
                                String shotby = jo.getString("ShotBy");
                                String killed = jo.getString("Killed");
                                if(killed.equals(username))
                                {
                                    Handler handler = new Handler(Looper.getMainLooper());
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getApplicationContext(),"You are dead! Login Back to take revenge! ;)",Toast.LENGTH_SHORT).show();
                                            Intent i = new Intent(Dashboard.this, MainActivity.class);
                                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                            startActivity(i);
                                            finish();
                                        }
                                    });
                                }
                                else
                                {
                                    showGameLog(killed + " killed by " + shotby);
                                }
                            }
                            else if(jo.getString("Event").equals("update")){
                                JSONObject jo2 = jo.getJSONObject("Clients");
                                JSONArray jsonArray = new Extra(getApplicationContext()).JSONObjectToArray(jo2);
                                updateMap(jsonArray);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    private void updateMap(final JSONArray ja)
    {
        new Thread(new Runnable() {
            public void run(){

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for(int i=0;i<ja.length();i++){
                            try {
                                String id = ja.getJSONObject(i).getString("id");
                                if(id!=username)
                                {
                                    JSONObject location = ja.getJSONObject(i).getJSONObject("location");
                                    final Double temp_long = location.getJSONObject("Location").getDouble("Long");
                                    final Double temp_lat = location.getJSONObject("Location").getDouble("Lat");
                                    googleMap.clear();
                                    MarkerOptions self_markerOptions;
                                    self_markerOptions = new MarkerOptions()
                                            .position(new LatLng(temp_lat, temp_long))
                                            .title("")
                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                                    marker = googleMap.addMarker(self_markerOptions);

                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                });

            }
        }).start();
    }

    private void showGameLog(String mymessage)
    {
        message = message + mymessage + "\n";
        gamelog.post(new Runnable() {
                          public void run() {
                              gamelog.setText(message);
                          }
                      });
    }

    private void moveToCurrentLocation(LatLng currentLocation)
    {
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation,18));
        // Zoom in, animating the camera.
//        googleMap.animateCamera(CameraUpdateFactory.zoomIn());
        // Zoom out to zoom level 10, animating with a duration of 2 seconds.
//        googleMap.animateCamera(CameraUpdateFactory.zoomTo(20), 2000, null);


    }

    public void shoot(View v){

        JSONObject jsonObject = new JSONObject();
        float temp_val = 0;
        if(event!=null)
        {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                mGravity = event.values;

            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                mGeomagnetic = event.values;

            if (mGravity != null && mGeomagnetic != null) {
                float R[] = new float[9];
                float I[] = new float[9];

                if (SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic)) {

                    // orientation contains azimut, pitch and roll
                    float orientation[] = new float[3];
                    SensorManager.getOrientation(R, orientation);

                    temp_val = orientation[0];
                }
            }
            try {
                jsonObject.put("id","123");
                jsonObject.put("event","shoot");
                JSONObject locationObj = new JSONObject();
                locationObj.put("lat",currentLatitude);
                locationObj.put("long",currentLongitude);
                jsonObject.put("location",locationObj);
                JSONObject axisObj = new JSONObject();
                axisObj.put("x",event.values[1]);
                axisObj.put("y",event.values[2]);
                axisObj.put("z",event.values[0]);
                axisObj.put("z2",temp_val);
//                Toast.makeText(getApplicationContext(), event.values[0] + "", Toast.LENGTH_LONG).show();
                jsonObject.put("axis",axisObj);
                sendSocket(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        new SoundEffects(getApplicationContext(),R.raw.gungunshot).execute();
    }

    /**
     * If locationChanges change lat and long
     *
     *
     * @param location
     */

    @Override
    public void onLocationChanged(Location location) {
        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();

        MarkerOptions self_markerOptions;

        self_markerOptions = new MarkerOptions()
                .position(new LatLng(currentLatitude, currentLongitude))
                .title("Your Location")
                .icon(BitmapDescriptorFactory.
                 defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

        if(marker!=null)
        {
            marker.remove();
            marker = null;
        }

        if(marker==null)
        {
            marker = googleMap.addMarker(self_markerOptions);
        }

        moveToCurrentLocation(new LatLng(currentLatitude, currentLongitude));

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id","123");
            jsonObject.put("event","update");
            JSONObject locationObj = new JSONObject();
            locationObj.put("lat",currentLatitude);
            locationObj.put("long",currentLongitude);
            jsonObject.put("location",locationObj);
            sendSocket(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.e("Test","here");
    }

    private void sendSocket(JSONObject jsonObject)
    {
        if(webSocket2!=null)
        {
            webSocket2.send(jsonObject.toString());
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        //if sensor is unreliable, return void
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
        {
            return;
        }
        this.event = event;
    }

    //Backside
    @Override
    protected void onResume() {
        super.onResume();
        //Now lets connect to the API
        mGoogleApiClient.connect();
        sManager.registerListener(this, sManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(this.getClass().getSimpleName(), "onPause()");

        //Disconnect from API onPause()
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        if(webSocket2!=null)
            webSocket2.close();
        sManager.unregisterListener(this);
    }

    //Extra Methods
    /**
     * If connected get lat and long
     *
     */
    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }


    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
            /*
             * Google Play services can resolve some errors it detects.
             * If the error has a resolution, try sending an Intent to
             * start a Google Play services activity that can resolve
             * error.
             */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
                    /*
                     * Thrown if Google Play services canceled the original
                     * PendingIntent
                     */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
                /*
                 * If no resolution is available, display a dialog to the
                 * user with the error.
                 */
            Log.e("Error", "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        this.googleMap = googleMap;
    }
}
