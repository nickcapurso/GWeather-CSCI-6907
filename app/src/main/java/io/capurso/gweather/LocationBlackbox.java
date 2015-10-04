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
    private static final String ZIPCODE_REGEX = "\\d{5}(-\\d{4})?";

    private static final String GPS_ONLY = "0";
    private static final String NETWORK_ONLY = "1";
    private static final String BOTH_GPS_NETWORK = "2";

    private SharedPreferences mSharedPrefs;
    private LocationManager mLocationManager;
    private Context mContext;
    private BlackboxListener mClient;

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

        }else if(locationMode.equals(GPS_ONLY)){
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
            mLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null);
            mLocationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null);
        }
    }

    private LocationWrapper extractLocation(String jsonData){
        JSONObject jsonParser, addressObj;
        JSONArray jsonArray = null;

        String address = "", status;
        double latitude, longitude;

        Location location = new Location("");

        latitude = longitude = 0;

        try {
            jsonParser = new JSONObject(jsonData);

            //Make sure the error status code isn't set
            //TODO string constants
            status = jsonParser.getString("status");
            if(!(status.equals("OK"))) {

                if(status.equals("ZERO_RESULTS"))
                    mClient.onBlackboxError(ErrorCodes.ERR_BAD_ZIP);
                else
                    mClient.onBlackboxError(ErrorCodes.ERR_JSON_FAILED);
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
        return new LocationWrapper(location, address);
    }

    private void verifyZipcode(String zipcode){
        if(!zipcode.matches(ZIPCODE_REGEX)){
            mClient.onBlackboxError(ErrorCodes.ERR_BAD_ZIP);
            return;
        }

        new JSONFetcher(this).execute(API_URLS.GEOCODING, API_URLS.GEOCODING_ADDR_PARAM, zipcode);
    }

    @Override
    public void onLocationChanged(Location location) {
        mLocationManager.removeUpdates(this);

        //TODO string constant
        mClient.onLocationFound(new LocationWrapper(location, "Current Location"));
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
        LocationWrapper location = extractLocation(result);

        if(location != null)
            mClient.onLocationFound(location);
    }

    class ErrorCodes{
        static final byte ERR_BAD_ZIP = 0x00;
        static final byte ERR_NETWORK_TIMEOUT = 0x01;
        static final byte ERR_JSON_FAILED = 0x02;
        static final byte ERR_GPS_DISABLED = 0x03;
        static final byte ERR_NETWORK_DISABLED = 0x04;
    }
}
