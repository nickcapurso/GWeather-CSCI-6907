package io.capurso.gweather;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.capurso.gweather.common.API_URLS;
import io.capurso.gweather.json.JSONEventListener;
import io.capurso.gweather.json.JSONFetcher;

import static io.capurso.gweather.common.Utils.DEBUG;

/**
 * Created by nickcapurso on 10/2/15.
 */
public class LocationBlackbox implements LocationListener, JSONEventListener {
    private static final String TAG = LocationBlackbox.class.getName();

    private static final String GPS_ONLY = "0";
    private static final String NETWORK_ONLY = "1";
    private static final String BOTH_GPS_NETWORK = "2";

    private String[] mPrefLocationValues;
    private SharedPreferences mSharedPrefs;
    private LocationManager mLocationManager;
    private Context mContext;
    private BlackboxListener mClient;

    public LocationBlackbox(Context context, BlackboxListener listener){
        mContext = context;
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mPrefLocationValues = mContext.getResources().getStringArray(R.array.pref_location_values);
        mClient = listener;
    }

    public void getLocation(){
        mLocationManager.removeUpdates(this);

        boolean zipcodeOverride = mSharedPrefs.getBoolean(mContext.getResources().getString(R.string.key_zipcode_use), false);
        String locationMode = mSharedPrefs.getString(mContext.getResources().getString(R.string.key_location_mode), GPS_ONLY);


        if(DEBUG) {
            Log.d(TAG, "zipcodeOverride = " + zipcodeOverride);
            Log.d(TAG, "locationMode = " + locationMode);
        }

        if(zipcodeOverride){
            String zipcode = mSharedPrefs.getString(mContext.getResources().getString(R.string.key_zipcode_set), "");

            if(zipcode.equals("")){
                //TODO
            }

            //TODO make string constant
            new JSONFetcher(this).execute(API_URLS.GEOCODING, "address", zipcode);
        }else if(locationMode.equals(GPS_ONLY)){
            mLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null);
        }else if(locationMode.equals(NETWORK_ONLY)){
            mLocationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null);
        }else if(locationMode.equals(BOTH_GPS_NETWORK)){
            mLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null);
            mLocationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null);
        }
    }

    private Location extractLocation(String jsonData){
        JSONObject jsonParser, addressObj;
        JSONArray jsonArray = null;

        String address;
        double latitude, longitude;

        Location location = new Location("");

        latitude = longitude = 0;

        try {
            jsonParser = new JSONObject(jsonData);

            //Make sure the error status code isn't set
            if(!(jsonParser.getString("status").equals("OK"))) {
                //TODO
                return null;
            }

            jsonArray = jsonParser.getJSONArray("results");

            addressObj = jsonArray.getJSONObject(0);
            address = addressObj.getString("formatted_address");
            latitude = addressObj.getJSONObject("geometry").getJSONObject("location").getDouble("lat");
            longitude = addressObj.getJSONObject("geometry").getJSONObject("location").getDouble("lng");

            if(DEBUG) Log.d(TAG, "Zipcode: " + address + ", lat: " + latitude + ", lng: " + longitude);
        } catch (JSONException e) {
            e.printStackTrace();

        }

        location.setLatitude(latitude);
        location.setLongitude(longitude);
        return location;
    }


    @Override
    public void onLocationChanged(Location location) {
        mLocationManager.removeUpdates(this);
        mClient.onLocationFound(location);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {}

    @Override
    public void onProviderEnabled(String s) {}

    @Override
    public void onProviderDisabled(String s) { }

    @Override
    public void onNetworkTimeout() {
        mClient.onBlackboxError();
    }

    @Override
    public void onJSONFetchErr() {
        mClient.onBlackboxError();
    }

    @Override
    public void onJSONFetchSuccess(String result) {
        mClient.onLocationFound(extractLocation(result));
    }

    interface BlackboxListener{
        public void onLocationFound(Location location);
        public void onBlackboxError();
    }
}
