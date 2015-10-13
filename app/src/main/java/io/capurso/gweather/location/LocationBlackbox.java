package io.capurso.gweather.location;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import io.capurso.gweather.R;
import io.capurso.gweather.common.API_URLS;
import io.capurso.gweather.common.Timeout;
import io.capurso.gweather.common.TimeoutListener;
import io.capurso.gweather.json.JSONEventListener;
import io.capurso.gweather.json.JSONFetcher;

import static io.capurso.gweather.common.Utils.DEBUG;

/**
 * Created by nickcapurso on 10/2/15.
 */
public class LocationBlackbox implements LocationListener, JSONEventListener {
    private static final String TAG = LocationBlackbox.class.getName();
    private static final String ZIPCODE_REGEX = "\\d{5}(-\\d{4})?";

    private static final String GPS_ONLY = "0";
    private static final String NETWORK_ONLY = "1";
    private static final String BOTH_GPS_NETWORK = "2";

    private SharedPreferences mSharedPrefs;
    private LocationManager mLocationManager;
    private Context mContext;
    private BlackboxListener mClient;
    private Timeout mLocationTimeout;

    public LocationBlackbox(Context context, BlackboxListener listener){
        mContext = context;
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mClient = listener;
    }

    public void requestLocation(){
        mLocationManager.removeUpdates(this);

        boolean zipcodeOverride = mSharedPrefs.getBoolean(mContext.getResources().getString(R.string.key_zipcode_use), false);
        String locationMode = mSharedPrefs.getString(mContext.getResources().getString(R.string.key_location_mode), GPS_ONLY);

        if(DEBUG) {
            Log.d(TAG, "zipcodeOverride = " + zipcodeOverride);
            Log.d(TAG, "locationMode = " + locationMode);
        }

        if(zipcodeOverride){
            String zipcode = mSharedPrefs.getString(mContext.getResources().getString(R.string.key_zipcode_set), "");
            verifyZipcode(zipcode);
            return;
        }

        if(locationMode.equals(GPS_ONLY)){
            if(!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                mClient.onBlackboxError(ErrorCodes.ERR_GPS_DISABLED);
                return;
            }

            mLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null);
        }else if(locationMode.equals(NETWORK_ONLY)){
            if(!mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
                mClient.onBlackboxError(ErrorCodes.ERR_NETWORK_DISABLED);
                return;
            }

            mLocationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null);
        }else if(locationMode.equals(BOTH_GPS_NETWORK)){
            if(!mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) &&
                    !mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                mClient.onBlackboxError(ErrorCodes.ERR_LOCATION_DISABLED);
                return;
            }

            mLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null);
            mLocationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null);
        }

        mLocationTimeout = new Timeout(new TimeoutListener() {
            @Override
            public void onTimeout() {
                mLocationManager.removeUpdates(LocationBlackbox.this);
                mClient.onBlackboxError(ErrorCodes.ERR_LOCATION_TIMEOUT);
            }
        });
        mLocationTimeout.start();
    }

    private LocationWrapper extractWundergroundLocation(String jsonData){
        JSONObject jsonTop, jsonLocation;
        String address = "", searchString = "";
        Location location = new Location("");
        double latitude, longitude;

        latitude = longitude = 0;

        try{
            jsonTop = new JSONObject(jsonData);
            jsonLocation = jsonTop.getJSONObject("location");
            searchString = jsonLocation.getString("city") + ", " + jsonLocation.getString("state");
            address = searchString + " " +
                    jsonLocation.getString("zip") + ", " + jsonLocation.getString("country_name");
            latitude = jsonLocation.getDouble("lat");
            longitude = jsonLocation.getDouble("lon");
        } catch (JSONException e) {
            mClient.onBlackboxError(ErrorCodes.ERR_BAD_ZIP);
            e.printStackTrace();
            return null;
        }

        location.setLatitude(latitude);
        location.setLongitude(longitude);
        return new LocationWrapper(location, address, searchString);
    }

    private void reverseGeocode(Location location){
        if(DEBUG) Log.d(TAG, "Starting reverse geocoding");

        //TODO use something mutable
        String url = API_URLS.WUNDERGROUND + API_URLS.WUNDERGROUND_REVERSE_GEOCODE;
        url += location.getLatitude() + "," + location.getLongitude();
        url += API_URLS.WUNDERGROUND_FORMAT;

        new JSONFetcher(this).execute(url);
    }


    private void verifyZipcode(String zipcode){
        if(!zipcode.matches(ZIPCODE_REGEX)){
            mClient.onBlackboxError(ErrorCodes.ERR_BAD_ZIP);
            return;
        }

        String url = API_URLS.WUNDERGROUND + API_URLS.WUNDERGROUND_REVERSE_GEOCODE;
        url += zipcode;
        url += API_URLS.WUNDERGROUND_FORMAT;

        new JSONFetcher(this).execute(url);
    }

    @Override
    public void onLocationChanged(Location location) {
        mLocationTimeout.cancel();
        mLocationManager.removeUpdates(this);
        reverseGeocode(location);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {}

    @Override
    public void onProviderEnabled(String s) {}

    @Override
    public void onProviderDisabled(String s) { }

    @Override
    public void onNetworkTimeout() {
        mClient.onBlackboxError(ErrorCodes.ERR_NETWORK_TIMEOUT);
    }

    @Override
    public void onJSONFetchErr() {
        if(DEBUG) Log.d(TAG, "Error fetching");
        mClient.onBlackboxError(ErrorCodes.ERR_JSON_FAILED);
    }

    @Override
    public void onJSONFetchSuccess(String result) {
        LocationWrapper location = extractWundergroundLocation(result);

        if(location != null)
            mClient.onLocationFound(location);
    }

    public static class ErrorCodes{
        public static final byte ERR_BAD_ZIP = 0x00;
        public static final byte ERR_NETWORK_TIMEOUT = 0x01;
        public static final byte ERR_JSON_FAILED = 0x02;
        public static final byte ERR_GPS_DISABLED = 0x03;
        public static final byte ERR_NETWORK_DISABLED = 0x04;
        public static final byte ERR_LOCATION_DISABLED = 0x05;
        public static final byte ERR_LOCATION_TIMEOUT = 0x06;
    }
}
