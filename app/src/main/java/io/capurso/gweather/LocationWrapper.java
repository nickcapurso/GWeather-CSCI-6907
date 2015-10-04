package io.capurso.gweather;

import android.location.Location;

/**
 * Created by Nick on 10/3/2015.
 */
public class LocationWrapper {
    public Location location;
    public String address;

    public LocationWrapper(Location location, String address){
        this.location = location;
        this.address = address;
    }
}
