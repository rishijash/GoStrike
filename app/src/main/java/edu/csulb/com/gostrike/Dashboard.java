package edu.csulb.com.gostrike;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
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
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Hashtable;
import java.util.Map;

import edu.csulb.com.gostrike.app.Extra;

import edu.csulb.com.gostrike.app.Player;
import edu.csulb.com.gostrike.app.SoundEffects;

public class Dashboard extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, SensorEventListener, OnMapReadyCallback {

    //Define a request code to send to Google Play services

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private double currentLatitude,currentLongitude;
    public WebSocket webSocket2=null;

    GoogleMap googleMap;
    Marker marker;
    String username,message;
    TextView gamelog;
    Extra extra;
    ImageView image;
    float currentDegree=0f;

    private SensorManager mSensorManager;
    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];
    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];
    private float bearing;
    private float mDeclination;
    private float mTargetDirection;
    private Map<String, Player> playerMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);


        //Initialize
        extra = new Extra(getApplicationContext());
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                // The next two lines tell the new client that “this” current class will handle connection stuff
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                //fourth line adds the LocationServices API endpoint from GooglePlayServices
                .addApi(LocationServices.API)
                .build();
        gamelog = (TextView) findViewById(R.id.gamelog);
        gamelog.setSelected(true);
        gamelog.setMovementMethod(new ScrollingMovementMethod());
        image = (ImageView) findViewById(R.id.imageViewCompass);

        playerMap = new Hashtable<>();

        message = "";

        SharedPreferences sf = getSharedPreferences("GoStrike",MODE_PRIVATE);
        username = sf.getString("username","Error");

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(500)        // 10 seconds, in milliseconds
                .setFastestInterval(200); // 1 second, in milliseconds

        //changed
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        socketConnection();
        showGameLog("Welcome Soldier!");
        mHandler.postDelayed(mCompassViewUpdater, 20);
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

    public void exitButton(View v)
    {
        exitPlay();
    }

    public void exitPlay()
    {
        Intent i = new Intent(Dashboard.this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
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
        mSensorManager.unregisterListener(this);
    }

    //On Shoot
    public void shoot(View v){

        JSONObject jsonObject = new JSONObject();
        float temp_val = 0;
        updateOrientationAngles();

        try {
            jsonObject.put("id","123");
            jsonObject.put("event","shoot");
            JSONObject locationObj = new JSONObject();
            locationObj.put("latitude",currentLatitude);
            locationObj.put("longitude",currentLongitude);
            jsonObject.put("location",locationObj);
            double temp = Math.toDegrees(mOrientationAngles[0]);
            jsonObject.put("z",temp);
            sendSocket(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        new SoundEffects(getApplicationContext(),R.raw.gungunshot).execute();
    }

    private void socketConnection()
    {
        AsyncHttpClient.getDefaultInstance().websocket("ws://"+extra.getIP()+":8080/join", "", new AsyncHttpClient.WebSocketConnectCallback() {
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
//                        System.out.println("I got a string: " + s);
                        try {
                            JSONObject jo = new JSONObject(s);
                            if(jo.getString("Event").equals("kill"))
                            {
                                String shotby = jo.getString("ShotBy");
                                final String killed = jo.getString("Killed");
                                if(killed.equalsIgnoreCase(username))
                                {
                                    Handler handler = new Handler(Looper.getMainLooper());
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            extra.setLost(extra.getLost()+1);
                                            Toast.makeText(getApplicationContext(),"You lost! Start Again to take revenge! ;)",Toast.LENGTH_SHORT).show();
                                            exitPlay();
                                        }
                                    });
                                }
                                else
                                {
                                    if(shotby.equalsIgnoreCase(username))
                                    {
                                        extra.setWon(extra.getWon()+1);
                                    }
                                }
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        playerMap.get(killed).getMarker().setVisible(false);
                                    }
                                });
                                showGameLog(killed + " killed by " + shotby);
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
                        Player player;
                        for(int i=0;i<ja.length();i++){
                            try {
                                String id = ja.getJSONObject(i).getString("id");

                                if(!id.equalsIgnoreCase(username))
                                {
                                    JSONObject location = ja.getJSONObject(i).getJSONObject("location");
                                    final Double temp_long = location.getJSONObject("Location").getDouble("Longitude");
                                    final Double temp_lat = location.getJSONObject("Location").getDouble("Latitude");
                                    player = playerMap.get(id);

                                    if (player == null) {
                                        MarkerOptions self_markerOptions = new MarkerOptions()
                                                .position(new LatLng(temp_lat, temp_long))
                                                .title(id)
                                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                                        Marker marker = googleMap.addMarker(self_markerOptions);
                                        player = new Player(id, marker);
                                        playerMap.put(id, player);
                                    }

                                    player.setLocation(temp_lat, temp_long);
//                                    googleMap.clear();
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

    private void moveToCurrentLocation(float mDirection)
    {
        CameraPosition newCamPos = new CameraPosition.Builder()
                .target(new LatLng(currentLatitude, currentLongitude))
                .bearing(mDirection).zoom(18f).build();

        if (googleMap != null) {
            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(newCamPos));
        }
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

        if(marker!=null)
        {
            marker.remove();
            marker = null;
        }

        GeomagneticField field = new GeomagneticField(
                (float)location.getLatitude(),
                (float)location.getLongitude(),
                (float)location.getAltitude(),
                System.currentTimeMillis()
        );

        // getDeclination returns degrees
        mDeclination = field.getDeclination();

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id","123");
            jsonObject.put("event","update");
            JSONObject locationObj = new JSONObject();
            locationObj.put("latitude",currentLatitude);
            locationObj.put("longitude",currentLongitude);
            jsonObject.put("location",locationObj);
            sendSocket(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
//        Log.e("Test","here");
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

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mAccelerometerReading,
                    0, mAccelerometerReading.length);
        }
        else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mMagnetometerReading,
                    0, mMagnetometerReading.length);
        }


        updateOrientationAngles();
    }

    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        mSensorManager.getRotationMatrix(mRotationMatrix, null,
                mAccelerometerReading, mMagnetometerReading);

        // "mRotationMatrix" now has up-to-date information.

        mSensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

        //Amimate
        bearing = (float) Math.toDegrees(mOrientationAngles[0]);
        mTargetDirection = normalizeDegree(bearing + 90);
//        gamelog.setText("" + bearing + " " + mTargetDirection);
        RotateAnimation ra = new RotateAnimation(
                currentDegree,
                -mTargetDirection,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);
        // how long the animation will take place
        ra.setDuration(210);
        // set the animation after the end of the reservation status
        ra.setFillAfter(true);
        // Start the animation
        image.startAnimation(ra);
        currentDegree = -mTargetDirection;

        // "mOrientationAngles" now has up-to-date information.
    }

    private float normalizeDegree(float degree) {
        return (degree + 720) % 360;
    }

    //Backside
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onResume() {
        super.onResume();
        //Now lets connect to the API
        mGoogleApiClient.connect();
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_FASTEST, SensorManager.SENSOR_DELAY_FASTEST);
    }


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
        googleMap.setBuildingsEnabled(true);
        enableMyLocation();
//        googleMap.se
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
        } else if (googleMap != null) {
            // Access to the location has been granted to the app.
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(false);
//            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLatitude, currentLongitude),18));
        }
    }

    protected final Handler mHandler = new Handler();
    private float mDirection;
    private final float MAX_ROATE_DEGREE = 1.0f;
    private AccelerateInterpolator mInterpolator = new AccelerateInterpolator();;
    protected Runnable mCompassViewUpdater = new Runnable() {
        @Override
        public void run() {

            if (mTargetDirection != mDirection) {

                // calculate the short routine
                float to = mTargetDirection;
                if (to - mDirection > 180) {
                    to -= 360;
                } else if (to - mDirection < -180) {
                    to += 360;
                }

                // limit the max speed to MAX_ROTATE_DEGREE
                float distance = to - mDirection;
                if (Math.abs(distance) > MAX_ROATE_DEGREE) {
                    distance = distance > 0 ? MAX_ROATE_DEGREE : (-1.0f * MAX_ROATE_DEGREE);
                }

                // need to slow down if the distance is short
                mDirection = normalizeDegree(mDirection
                        + ((to - mDirection) * mInterpolator.getInterpolation(Math
                        .abs(distance) > MAX_ROATE_DEGREE ? 0.4f : 0.3f)));
                moveToCurrentLocation(mDirection);
            }

            mHandler.postDelayed(mCompassViewUpdater, 20);
        }
    };
}
