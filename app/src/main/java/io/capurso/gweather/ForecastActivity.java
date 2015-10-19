package io.capurso.gweather;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import io.capurso.gweather.common.Utils;
import io.capurso.gweather.location.BlackboxListener;
import io.capurso.gweather.location.LocationBlackbox;
import io.capurso.gweather.location.LocationWrapper;
import io.capurso.gweather.weather.WeatherListener;
import io.capurso.gweather.weather.WeatherManager;
import io.capurso.gweather.weather.forecast.ForecastAdapter;
import io.capurso.gweather.weather.forecast.ForecastInfo;
import io.capurso.gweather.weather.forecast.ForecastViewClickListener;

import static io.capurso.gweather.common.Utils.DEBUG;

/**
 * The Activity which is responsible for display the current weather to the user. Delegates the tasks of location
 * finding and weather information retrieval out to their respective classes/managers.
 */
public class ForecastActivity extends AppCompatActivity implements BlackboxListener, WeatherListener, ForecastViewClickListener {
    private static final String TAG = ForecastActivity.class.getName();

    /**
     * Animation delay for the "stagger" effect when displaying the forecast.
     */
    private static final long FORECAST_VIEW_DELAY = 300; //300 ms

    //onSaveInstanceState Bundle keys
    private static final String BUNDLE_KEY_FORECAST_LIST = "forecastList";
    private static final String BUNDLE_KEY_LOCATION_TITLE = "locationTitle";
    private static final String BUNDLE_KEY_LOCATION_WRAPPER = "locationWrapper";
    private static final String BUNDLE_KEY_CURR_TEMP = "currTemp";
    private static final String BUNDLE_KEY_LOADING = "loadingForecast";

    /**
     * Reference to the top layout. Mainly used for creating Snackbar messages.
     */
    private LinearLayout mMainLayout;

    /**
     * Material Design's "efficient" ListView (uses the ViewHolder pattern). In my app,
     * it is used to display the daily forecast.
     */
    private RecyclerView mRvForecast;

    /**
     * LayoutManager for the RecyclerView (either display list items vertically or horizontally).
     */
    private LinearLayoutManager mRecyclerLayoutManager;

    /**
     * List adapter for forecast items.
     */
    private ForecastAdapter mAdapter;

    /**
     * Manager to delegate the task of getting weather information to.
     */
    private WeatherManager mWeatherManager;

    /**
     * Manager to delegate the task of mananging the "banner" (location image, current temp, and location description)
     */
    private BannerManager mBannerManager;

    /**
     * List of forecast information items.
     */
    private ArrayList<ForecastInfo> mForecastInfo;

    /**
     * Reference to the Handler for the UI Thread. In this case, I use add ForecastInfo items
     * to mForecastInfo using a delay (to create the stagger animation effect).
     */
    private Handler mHandler;

    /**
     * Wrapper for the current location, so the location information can persist across screen rotations.
     */
    private LocationWrapper mCurrLocation;

    /**
     * The current temperature. Kept as an instance variable so it can persist across screen rotations.
     */
    private String mCurrTemp;

    /**
     * The progress dialog that displays while waiting for the forecast to load.
     */
    private ProgressDialog mProgressDialog;

    /**
     * Keeps track of whether or not we are waiting for weather information to be retrieved.
     */
    private boolean mLoadingData;

    /**
     * Used to delegate the task of determining the user's location.
     */
    private LocationBlackbox mLocationBlackbox;

    /**
     * Set up widgets and restore state after a screen rotation.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecast);

        //Set up pre-Lollipop ActionBar
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        mMainLayout = (LinearLayout)findViewById(R.id.llForecast);
        mForecastInfo = new ArrayList<ForecastInfo>();

        mRvForecast = (RecyclerView) findViewById(R.id.rvForecast);
        mRvForecast.setHasFixedSize(true);

        mRecyclerLayoutManager = new LinearLayoutManager(this);

        //Want the RecyclerView to be horizontal in landscape mode, but vertical in portrait mode
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            mRecyclerLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        else
            mRecyclerLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        mRvForecast.setLayoutManager(mRecyclerLayoutManager);

        mAdapter = new ForecastAdapter(this, mForecastInfo, this);
        mRvForecast.setAdapter(mAdapter);

        mHandler = new Handler();

        mBannerManager = new BannerManager(this, (ImageView)findViewById(R.id.ivLocationBanner), (TextView)findViewById(R.id.tvCurrentTemp));

        //Check if onCreate is being called after a screen rotation.
        if(savedInstanceState != null){
            //First check if the screen was rotated while loading the forecast. If so, restart the
            //process.
            if(savedInstanceState.getBoolean(BUNDLE_KEY_LOADING)){
                if(DEBUG) Log.d(TAG, "Was in the middle of loading forecast, restarting");
                refreshForecast();
                return;
            }

            //See if we previously had a forecast
            ArrayList<ForecastInfo> temp = savedInstanceState.getParcelableArrayList(BUNDLE_KEY_FORECAST_LIST);

            if(temp == null)
                return;

            //Restore the current location
            mCurrLocation = (LocationWrapper)savedInstanceState.getParcelable(BUNDLE_KEY_LOCATION_WRAPPER);

            //Restore the current temperature
            mCurrTemp = savedInstanceState.getString(BUNDLE_KEY_CURR_TEMP);

            //Restore the WeatherManager
            mWeatherManager = new WeatherManager(this, mCurrLocation.location, this);
            mWeatherManager.setForecastInfos(temp);

            //If in portrait mode, restore the banner
            if(getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                mBannerManager.setupBanner(mCurrLocation);
                mBannerManager.setCurrentTemp(mCurrTemp);
            }

            //Stagger in the forecast
            staggerInForecast(temp);

            //Show the location title and grey divider line
            TextView tvLocationName = ((TextView)findViewById(R.id.tvLocationName));
            View dividerView = findViewById(R.id.dividerView);

            tvLocationName.setVisibility(View.VISIBLE);
            dividerView.setVisibility(View.VISIBLE);

            //Restore the location title
            ((TextView)findViewById(R.id.tvLocationName)).setText(savedInstanceState.getString(BUNDLE_KEY_LOCATION_TITLE));

            return;
        }

        //If we reach this point, then this is not after a screen rotation and thus is likely
        //the first run of the app. Start fetching weather information automatically.
        refreshForecast();
    }

    /**
     * Save state before screen rotation. This includes canceling any current network or
     * location requests, then saving away the current forecast, location and temperature.
     * @param toSave
     */
    @Override
    public void onSaveInstanceState(Bundle toSave){
        super.onSaveInstanceState(toSave);
        //Cancel the loading dialog (if applicable)
        if(mProgressDialog != null)
            mProgressDialog.cancel();

        //Cancel any network requests the WeatherManager is making
        if(mWeatherManager != null)
            mWeatherManager.cancel();

        //Cancel any location or network requests from the LocationBlackbox
        if(mLocationBlackbox != null)
            mLocationBlackbox.cancel();

        //If we haven't found a forecast yet, it's possible that the screen is rotating
        //in the middle of retrieving it. Save the loading boolean.
        if(mForecastInfo.size() == 0){
            toSave.putBoolean(BUNDLE_KEY_LOADING, mLoadingData);
            return;
        }
        String locationTitle = ((TextView)findViewById(R.id.tvLocationName)).getText().toString();

        toSave.putParcelableArrayList(BUNDLE_KEY_FORECAST_LIST, mForecastInfo);
        toSave.putString(BUNDLE_KEY_LOCATION_TITLE, locationTitle);
        toSave.putParcelable(BUNDLE_KEY_LOCATION_WRAPPER, mCurrLocation);
        toSave.putString(BUNDLE_KEY_CURR_TEMP, mCurrTemp);
        toSave.putBoolean(BUNDLE_KEY_LOADING, mLoadingData);
    }

    /**
     * Called when the weather forecast has been found. Display it to the user, where each
     * daily forecast is presented in a list.
     * @param forecast
     */
    @Override
    public void onForecastReceived(ArrayList<ForecastInfo> forecast) {
        setNotLoadingState(); //Cancel the progress dialog
        staggerInForecast(forecast); //Animate the forecast entering the screen

        //Show the location title and grey divider line
        TextView tvLocationName = ((TextView)findViewById(R.id.tvLocationName));
        View dividerView = findViewById(R.id.dividerView);

        tvLocationName.setVisibility(View.VISIBLE);
        dividerView.setVisibility(View.VISIBLE);
        ((TextView)findViewById(R.id.tvLocationName)).setText(mCurrLocation.address);

        //Start the process to get the current temperature. It requires a different
        //URL than the weather forecast.
        mWeatherManager.requestCurrentTemp();
    }

    /**
     * Current temperature has been found. Display it if we're on portrait mode.
     * @param temp The current temperature.
     */
    @Override
    public void onCurrentTempReceived(String temp) {
        mCurrTemp = mWeatherManager.getCurrTemp();

        //Display the temperature in the "banner" if we're in portrait mode.
        if(getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE)
            mBannerManager.setCurrentTemp(mCurrTemp);
    }

    /**
     * If there is an error getting the weather, pop up a Snackbar and let the user
     * retry the process if desired.
     * @param errorCode Codes defined in WeatherManager.
     */
    @Override
    public void onWeatherError(byte errorCode) {
        setNotLoadingState();

        switch (errorCode){
            case WeatherManager.ErrorCodes.ERR_JSON_FAILED:
            case WeatherManager.ErrorCodes.ERR_NETWORK_TIMEOUT:
                String message = getString(R.string.error_internet);

                Snackbar retryBar = Snackbar.make(mMainLayout, message, Snackbar.LENGTH_LONG);
                retryBar.setAction(getString(R.string.retry), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mWeatherManager.requestForecast(); //Try to request the forecast again
                    }
                });

                retryBar.show();
                break;
        }
    }

    /**
     * By default, aslide-in animation is played when a new item is added to the RecyclerView.
     * To create the "stagger" effect, add them at different intervals.
     * @param forecast
     */
    private void staggerInForecast(ArrayList<ForecastInfo> forecast){
        long uptime = SystemClock.uptimeMillis() + FORECAST_VIEW_DELAY;
        for(final ForecastInfo info : forecast){
            mHandler.postAtTime(new Runnable() {
                @Override
                public void run() {
                    mForecastInfo.add(info);
                    mAdapter.notifyDataSetChanged();
                }
            }, uptime += FORECAST_VIEW_DELAY); //Add each View 300ms apart
        }
    }

    /**
     * The user's location has been found. Start the weather retrieval process.
     * @param location Found by the LocationBlackbox.
     */
    @Override
    public void onLocationFound(LocationWrapper location) {
        //Set the banner if we're in portrait mode
        if(getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE)
            mBannerManager.setupBanner(location);

        mWeatherManager = new WeatherManager(this, location.location, this);
        mWeatherManager.requestForecast();

        mCurrLocation = location;
    }

    /**
     * There are a few errors that the LocationBlackbox can raise. For each major type,
     * pop up a Snackbar so the user can fix the issue.
     * @param errCode Codes defined in the LocationBlackbox class.
     */
    @Override
    public void onBlackboxError(byte errCode) {
        setNotLoadingState();

        switch (errCode){
            //The user inputted a bad zipcode. The Snackbar will give the user the option to go into the
            //SettingsActivity and fix the zipcode.
            case LocationBlackbox.ErrorCodes.ERR_BAD_ZIP:
                Snackbar fixBar = Snackbar.make(mMainLayout, getString(R.string.error_zipcode), Snackbar.LENGTH_LONG);
                fixBar.setAction(getString(R.string.fix), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(new Intent(ForecastActivity.this, SettingsActivity.class));
                    }
                });

                fixBar.show();
                break;

            //For network-based errors, the Snackbar will give the user the option to
            //retry the request.
            case LocationBlackbox.ErrorCodes.ERR_JSON_FAILED:
            case LocationBlackbox.ErrorCodes.ERR_NETWORK_TIMEOUT:
            case LocationBlackbox.ErrorCodes.ERR_LOCATION_TIMEOUT:
                String message = errCode == LocationBlackbox.ErrorCodes.ERR_LOCATION_TIMEOUT?
                        getString(R.string.error_determine_location) : getString(R.string.error_internet);

                Snackbar retryBar = Snackbar.make(mMainLayout, message, Snackbar.LENGTH_LONG);
                retryBar.setAction(getString(R.string.retry), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ( mLocationBlackbox = new LocationBlackbox(ForecastActivity.this, ForecastActivity.this)).requestLocation();
                    }
                });

                retryBar.show();
                break;

            //For errors that occur because certain location features are disabled, the Snackbar
            //will give the user the option to go into the system settings to adjust location settings.
            case LocationBlackbox.ErrorCodes.ERR_GPS_DISABLED:
            case LocationBlackbox.ErrorCodes.ERR_NETWORK_DISABLED:
            case LocationBlackbox.ErrorCodes.ERR_LOCATION_DISABLED:
                String provider;

                //Customize the error message based on which provider is disabled.
                if(errCode != LocationBlackbox.ErrorCodes.ERR_LOCATION_DISABLED)
                    provider = errCode == LocationBlackbox.ErrorCodes.ERR_GPS_DISABLED ? getString(R.string.gps_location) : getString(R.string.network_location);
                else
                    provider = getString(R.string.location_services);

                Snackbar locationBar = Snackbar.make(mMainLayout, provider + getString(R.string.is_not_enabled), Snackbar.LENGTH_LONG);
                locationBar.setAction(getString(R.string.fix), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent, Utils.CODE_ENABLE_LOCATION);
                    }
                });

                locationBar.show();
                break;
        }
    }

    /**
     * Inflate the menu; this adds items to the action bar if it is present.
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_forecast, menu);
        return true;
    }

    /**
     * Two options are available: one to refresh the forecast and
     * the other to open the app's settings.
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            if(mLoadingData)
                return false;
            refreshForecast();
            return true;
        }else if(id == R.id.action_settings){
            startActivityForResult(new Intent(this, SettingsActivity.class), 0);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * When the user returns from either the system settings or the app settings, we
     * check whether or not the forecast needs to be refreshed. For example, if they
     * changed the temperature metric from Fahrenheit to Celsius.
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(DEBUG) Log.d(TAG, "Received code: " + resultCode);

        if(requestCode == Utils.CODE_ENABLE_LOCATION || resultCode == Utils.CODE_REFRESH_FORECAST)
            refreshForecast();
    }

    /**
     * When a forecast item was clicked, tell the WeatherManager to popup a detailed
     * information dialog if we're in portrait mode.
     * @param index The position of the clicked item.
     */
    @Override
    public void onForecastViewClicked(int index) {
        if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE)
            mWeatherManager.getDetailDialog(index, this).show();
    }

    /**
     * Starts the process to get the user's location. This will chain into getting the weather
     * information once the location is found.
     *
     * Also clears the current forecast information.
     */
    private void refreshForecast(){
        setLoadingState();
        mForecastInfo.clear();
        mAdapter.notifyDataSetChanged();
        (mLocationBlackbox = new LocationBlackbox(this, this)).requestLocation();
        mAdapter.resetAnimationCount();

        if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE)
            mBannerManager.hideCurrentTemp();
    }

    /**
     * Starts the progress dialog while the user is waiting for weather information.
     */
    private void setLoadingState(){
        mLoadingData = true;
        mProgressDialog = ProgressDialog.show(this, getString(R.string.please_wait), getString(R.string.loading_forecast), true);
    }

    /**
     * Stops the progress dialog when the weather information has been found or there was an error.
     */
    private void setNotLoadingState(){
        mLoadingData = false;
        mProgressDialog.cancel();
    }


    /* ------------------------------------------------------------------------------------------------
     * Getters and Setters - Mainly used while experimenting with unit tests
     * ------------------------------------------------------------------------------------------------ */
    public void setLocationTitle(String title){
        ((TextView)findViewById(R.id.tvLocationName)).setText(title);
    }

    public ForecastAdapter getAdapter() {
        return mAdapter;
    }

    public WeatherManager getWeatherManager() {
        return mWeatherManager;
    }

    public void setWeatherManager(WeatherManager weatherManager) {
        mWeatherManager = weatherManager;
    }

    public BannerManager getBannerManager() {
        return mBannerManager;
    }

    public void setBannerManager(BannerManager bannerManager) {
        mBannerManager = bannerManager;
    }

    public ArrayList<ForecastInfo> getForecastInfo() {
        return mForecastInfo;
    }

    public void setForecastInfo(ArrayList<ForecastInfo> forecastInfo) {
        mForecastInfo = forecastInfo;
    }

    public void setCurrLocation(LocationWrapper currLocation) {
        mCurrLocation = currLocation;
    }

    public void setCurrTemp(String currTemp) {
        mCurrTemp = currTemp;
    }

    public LinearLayoutManager getRecyclerLayoutManager() {
        return mRecyclerLayoutManager;
    }

}
