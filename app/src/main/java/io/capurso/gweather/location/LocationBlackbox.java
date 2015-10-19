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
 * Handles the task of determining the user's location, either by the hardware GPS or
 * network, or by resolving the inputted zipcode.
 */
public class LocationBlackbox implements LocationListener, JSONEventListener {
    private static final String TAG = LocationBlackbox.class.getName();

    /**
     * Regex to detect a valid zipcode format (5 digits and optional 4 digit extension).
     */
    private static final String ZIPCODE_REGEX = "\\d{5}(-\\d{4})?";

    /**
     * Prefeneces code for using only the GPS.
     */
    private static final String GPS_ONLY = "0";

    /**
     * Prefeneces code for using only the network.
     */
    private static final String NETWORK_ONLY = "1";

    /**
     * Prefeneces code for using both the GPS and network.
     */
    private static final String BOTH_GPS_NETWORK = "2";

    /**
     * Reference to the preference file for reading location mode settings.
     */
    private SharedPreferences mSharedPrefs;

    /**
     * Reference to the system service for obtaining location via GPS or network.
     */
    private LocationManager mLocationManager;

    /**
     * Used to get a handle on the location service.
     */
    private Context mContext;

    /**
     * The client listener to send callbacks to.
     */
    private BlackboxListener mClient;

    /**
     * Timer to detect if location resolution is taking too long.
     */
    private Timeout mLocationTimeout;

    /**
     * Makes network requests to resolve the inputted zipcode.
     */
    private JSONFetcher mJSONFetcher;

    /**
     * Sole instructor takes in context and a listener for callbacks.
     * @param context Used to get a handle on the location service.
     * @param listener Used to send callbacks to.
     */
    public LocationBlackbox(Context context, BlackboxListener listener){
        mContext = context;
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mClient = listener;
    }

    /**
     * Called by a client to begin the location-finding process. If the user is using a zipcode
     * in the preferences, then a network request is started to resolve the zipcode. Otherwise,
     * the LocationManager is set to start detecting the user's location.
     */
    public void requestLocation(){
        //Cancel any previous location requests.
        mLocationManager.removeUpdates(this);

        //Get zipcode vs. hardware location preference
        boolean zipcodeOverride = mSharedPrefs.getBoolean(mContext.getResources().getString(R.string.key_zipcode_use), false);
        String locationMode = mSharedPrefs.getString(mContext.getResources().getString(R.string.key_location_mode), GPS_ONLY);

        if(DEBUG) {
            Log.d(TAG, "zipcodeOverride = " + zipcodeOverride);
            Log.d(TAG, "locationMode = " + locationMode);
        }

        //For zipcode, call a method to verify a valid zipcode format and start the network request.
        if(zipcodeOverride){
            String zipcode = mSharedPrefs.getString(mContext.getResources().getString(R.string.key_zipcode_set), "");
            verifyZipcode(zipcode);
            return;
        }

        //For GPS location, ensure the provider is enabled before making a single request.
        if(locationMode.equals(GPS_ONLY)){
            if(!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                mClient.onBlackboxError(ErrorCodes.ERR_GPS_DISABLED); //Error callback
                return;
            }
            mLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null);

        //For network location, ensure the provider is enabled before making a single request.
        }else if(locationMode.equals(NETWORK_ONLY)){
            if(!mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
                mClient.onBlackboxError(ErrorCodes.ERR_NETWORK_DISABLED); //Error callback
                return;
            }
            mLocationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null);

        //For both GPS and network location, make sure either one is enabled, then make
        //a request for both types of location.
        }else if(locationMode.equals(BOTH_GPS_NETWORK)){
            if(!mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) &&
                    !mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                mClient.onBlackboxError(ErrorCodes.ERR_LOCATION_DISABLED); //Error callback
                return;
            }

            mLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null);
            mLocationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null);
        }

        //Start a timer for finding location.
        mLocationTimeout = new Timeout(new TimeoutListener() {
            @Override
            public void onTimeout() {
                mLocationManager.removeUpdates(LocationBlackbox.this); //Cancel location updates
                mClient.onBlackboxError(ErrorCodes.ERR_LOCATION_TIMEOUT); //Error callback
            }
        });
        mLocationTimeout.start();
    }

    /**
     * Using the Wunderground API for reverse geocoding.
     *      Sequence to lat/long = Top Level Object -> location -> [lat, lon]
     *
     * Also grabs the city/state/country so that a textual representation of the coordinates can
     * be presented to the user later. Finally, builds a String to be used by a image search
     * engine to get a picture of the location.
     *
     * @param jsonData The JSON result for the inputted zipcode.
     * @return A wrapped {Location location, String textualDescription, String imageSearchString}
     */
    private LocationWrapper extractWundergroundLocation(String jsonData){
        JSONObject jsonTop, jsonLocation;
        String address = "", searchString = "";
        Location location = new Location("");
        double latitude, longitude;

        try{
            jsonTop = new JSONObject(jsonData);

            //If the user entered not-actual zipcode, use the error callback.
            if(jsonTop.has("error")){
                mClient.onBlackboxError(ErrorCodes.ERR_BAD_ZIP);
                return null;
            }

            jsonLocation = jsonTop.getJSONObject("location");
            searchString = jsonLocation.getString("city") + ", " + jsonLocation.getString("state");
            address = searchString + " " +
                    jsonLocation.getString("zip") + ", " + jsonLocation.getString("country_name");
            latitude = jsonLocation.getDouble("lat");
            longitude = jsonLocation.getDouble("lon");
        } catch (JSONException e) {
            mClient.onBlackboxError(ErrorCodes.ERR_JSON_FAILED);
            e.printStackTrace();
            return null;
        }

        //Create the actual Location object.
        location.setLatitude(latitude);
        location.setLongitude(longitude);

        //Wrap the Location object together with a textual description and an image search engine string
        return new LocationWrapper(location, address, searchString);
    }

    /**
     * When a location is found, start the reverse geocoding process so we can have a name
     * of the current location (for finding an image later as well as presenting the location to the user).
     * The Wunderground API is used for this purpose.
     * @param location Found by the operating system.
     */
    private void reverseGeocode(Location location){
        if(DEBUG) Log.d(TAG, "Starting reverse geocoding");

        //TODO use something mutable
        //Set up reverse geocoding URL.
        String url = API_URLS.WUNDERGROUND + API_URLS.WUNDERGROUND_REVERSE_GEOCODE;
        url += location.getLatitude() + "," + location.getLongitude();
        url += API_URLS.WUNDERGROUND_FORMAT;

        //Start network AsyncTask
        (mJSONFetcher = new JSONFetcher(this)).execute(url);;
    }

    /**
     * Before starting the network request to resolve the zipcode, make sure it has the
     * format of an actual zipcode.
     * @param zipcode
     */
    private void verifyZipcode(String zipcode){
        //Regex matching
        if(!zipcode.matches(ZIPCODE_REGEX)){
            mClient.onBlackboxError(ErrorCodes.ERR_BAD_ZIP); //Error callback
            return;
        }

        String url = API_URLS.WUNDERGROUND + API_URLS.WUNDERGROUND_REVERSE_GEOCODE;
        url += zipcode;
        url += API_URLS.WUNDERGROUND_FORMAT;

        //Start network AsyncTask to turn the zipcode into actual GPS coordinates.
        (mJSONFetcher = new JSONFetcher(this)).execute(url);
    }

    /**
     * Cancel any network requests and location updates. For example, before a screen rotation.
     */
    public void cancel(){
        if(mJSONFetcher != null)
            mJSONFetcher.cancel(true);
        mLocationManager.removeUpdates(this);
    }

    /**
     * Location fix found. Start the reverse geocoding so we can have an image of the
     * location as well as its proper name.
     * @param location Found by operating system.
     */
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

    /**
     * In the event a network request fails, alert the client over the interface.
     */
    @Override
    public void onNetworkTimeout() {
        mClient.onBlackboxError(ErrorCodes.ERR_NETWORK_TIMEOUT);
    }

    /**
     * In the event a network request fails, alert the client over the interface.
     */
    @Override
    public void onJSONFetchErr() {
        if(DEBUG) Log.d(TAG, "Error fetching");
        mClient.onBlackboxError(ErrorCodes.ERR_JSON_FAILED);
    }

    /**
     * Now we have the JSON data containing the GPS coordinates for a zipcode. Extract it
     * and then send the data to the client listener over the interface.
     */
    @Override
    public void onJSONFetchSuccess(String result) {
        LocationWrapper location = extractWundergroundLocation(result);

        if(location != null)
            mClient.onLocationFound(location);
    }

    /**
     * Various error codes sent when the callback onBlackboxError is called.
     */
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
