package edu.csulb.com.gostrike.app;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by Admin on 5/12/2017.
 */

public class Player {
    private String name;
//    private Double latitude;
//    private Double longitude;
    private Marker marker;

    public Player(String name, Marker marker) {
        this.name = name;
//        this.latitude = latitude;
//        this.longitude = longitude;
        this.marker = marker;
    }

    public void setLocation(double latitude, double longitude) {
        marker.setPosition(new LatLng(latitude, longitude));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

//    public Double getLatitude() {
//        return latitude;
//    }
//
//    public void setLatitude(Double latitude) {
//        this.latitude = latitude;
//    }
//
//    public Double getLongitude() {
//        return longitude;
//    }
//
//    public void setLongitude(Double longitude) {
//        this.longitude = longitude;
//    }

    public Marker getMarker() {
        return marker;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }
}
