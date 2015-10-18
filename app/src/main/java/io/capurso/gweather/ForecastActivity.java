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

public class ForecastActivity extends AppCompatActivity implements BlackboxListener, WeatherListener, ForecastViewClickListener {
    private static final String TAG = ForecastActivity.class.getName();
    private static final long FORECAST_VIEW_DELAY = 300; //300 ms

    private static final String BUNDLE_KEY_FORECAST_LIST = "forecastList";
    private static final String BUNDLE_KEY_LOCATION_TITLE = "locationTitle";
    private static final String BUNDLE_KEY_LOCATION_WRAPPER = "locationWrapper";
    private static final String BUNDLE_KEY_CURR_TEMP = "currTemp";
    private static final String BUNDLE_KEY_LOADING = "loadingForecast";

    private LinearLayout mMainLayout;

    private RecyclerView mRvForecast;

    private LinearLayoutManager mRecyclerLayoutManager;
    private ForecastAdapter mAdapter;

    private WeatherManager mWeatherManager;
    private BannerManager mBannerManager;
    private ArrayList<ForecastInfo> mForecastInfo;

    private Handler mHandler;
    private LocationWrapper mCurrLocation;
    private String mCurrTemp;

    private ProgressDialog mProgressDialog;
    private boolean mLoadingData;
    private LocationBlackbox mLocationBlackbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecast);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        mMainLayout = (LinearLayout)findViewById(R.id.llForecast);
        mForecastInfo = new ArrayList<ForecastInfo>();

        mRvForecast = (RecyclerView) findViewById(R.id.rvForecast);
        mRvForecast.setHasFixedSize(true);

        mRecyclerLayoutManager = new LinearLayoutManager(this);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            mRecyclerLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        else
            mRecyclerLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        mRvForecast.setLayoutManager(mRecyclerLayoutManager);

        mAdapter = new ForecastAdapter(this, mForecastInfo, this);
        mRvForecast.setAdapter(mAdapter);

        mHandler = new Handler();

        mBannerManager = new BannerManager(this, (ImageView)findViewById(R.id.ivLocationBanner), (TextView)findViewById(R.id.tvCurrentTemp));

        if(savedInstanceState != null){
            if(savedInstanceState.getBoolean(BUNDLE_KEY_LOADING)){
                if(DEBUG) Log.d(TAG, "Was in the middle of loading forecast, restarting");
                refreshForecast();
                return;
            }

            ArrayList<ForecastInfo> temp = savedInstanceState.getParcelableArrayList(BUNDLE_KEY_FORECAST_LIST);

            if(temp == null)
                return;

            mCurrLocation = (LocationWrapper)savedInstanceState.getParcelable(BUNDLE_KEY_LOCATION_WRAPPER);
            mCurrTemp = savedInstanceState.getString(BUNDLE_KEY_CURR_TEMP);

            mWeatherManager = new WeatherManager(this, mCurrLocation.location, this);
            mWeatherManager.setForecastInfos(temp);

            if(getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                mBannerManager.setupBanner(mCurrLocation);
                mBannerManager.setCurrentTemp(mCurrTemp);
            }

            staggerInForecast(temp);

            TextView tvLocationName = ((TextView)findViewById(R.id.tvLocationName));
            View dividerView = findViewById(R.id.dividerView);

            tvLocationName.setVisibility(View.VISIBLE);
            dividerView.setVisibility(View.VISIBLE);
            ((TextView)findViewById(R.id.tvLocationName)).setText(savedInstanceState.getString(BUNDLE_KEY_LOCATION_TITLE));

            return;
        }

        refreshForecast();
    }

    @Override
    public void onSaveInstanceState(Bundle toSave){
        super.onSaveInstanceState(toSave);
        if(mProgressDialog != null)
            mProgressDialog.cancel();

        if(mWeatherManager != null)
            mWeatherManager.cancel();

        if(mLocationBlackbox != null)
            mLocationBlackbox.cancel();

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


    @Override
    public void onForecastReceived(ArrayList<ForecastInfo> forecast) {
        setNotLoadingState();
        staggerInForecast(forecast);

        TextView tvLocationName = ((TextView)findViewById(R.id.tvLocationName));
        View dividerView = findViewById(R.id.dividerView);

        tvLocationName.setVisibility(View.VISIBLE);
        dividerView.setVisibility(View.VISIBLE);
        ((TextView)findViewById(R.id.tvLocationName)).setText(mCurrLocation.address);

        mWeatherManager.requestCurrentTemp();
    }

    @Override
    public void onCurrentTempReceived(String temp) {
        mCurrTemp = mWeatherManager.getCurrTemp();

        if(getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE)
            mBannerManager.setCurrentTemp(mCurrTemp);
    }

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
                        mWeatherManager.requestForecast();
                    }
                });

                retryBar.show();

                break;
        }
    }

    private void staggerInForecast(ArrayList<ForecastInfo> forecast){
        long uptime = SystemClock.uptimeMillis() + FORECAST_VIEW_DELAY;
        for(final ForecastInfo info : forecast){
            mHandler.postAtTime(new Runnable() {
                @Override
                public void run() {
                    mForecastInfo.add(info);
                    mAdapter.notifyDataSetChanged();
                }
            }, uptime += FORECAST_VIEW_DELAY);
        }
    }

    @Override
    public void onLocationFound(LocationWrapper location) {
        if(getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE)
            mBannerManager.setupBanner(location);

        mWeatherManager = new WeatherManager(this, location.location, this);
        mWeatherManager.requestForecast();

        mCurrLocation = location;
    }

    @Override
    public void onBlackboxError(byte errCode) {
        setNotLoadingState();

        switch (errCode){
            case LocationBlackbox.ErrorCodes.ERR_BAD_ZIP:
                //TODO string constants
                Snackbar fixBar = Snackbar.make(mMainLayout, getString(R.string.error_zipcode), Snackbar.LENGTH_LONG);
                fixBar.setAction(getString(R.string.fix), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(new Intent(ForecastActivity.this, SettingsActivity.class));
                    }
                });

                fixBar.show();
                break;
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

            case LocationBlackbox.ErrorCodes.ERR_GPS_DISABLED:
            case LocationBlackbox.ErrorCodes.ERR_NETWORK_DISABLED:
            case LocationBlackbox.ErrorCodes.ERR_LOCATION_DISABLED:
                String provider;
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_forecast, menu);
        return true;
    }

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(DEBUG) Log.d(TAG, "Received code: " + resultCode);

        if(requestCode == Utils.CODE_ENABLE_LOCATION || resultCode == Utils.CODE_REFRESH_FORECAST)
            refreshForecast();
    }


    @Override
    public void onForecastViewClicked(int index) {
        if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE)
            mWeatherManager.getDetailDialog(index, this).show();
    }

    private void refreshForecast(){
        setLoadingState();
        mForecastInfo.clear();
        mAdapter.notifyDataSetChanged();
        (mLocationBlackbox = new LocationBlackbox(this, this)).requestLocation();
        mAdapter.resetAnimationCount();

        if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE)
            mBannerManager.hideCurrentTemp();
    }

    private void setLoadingState(){
        mLoadingData = true;
        mProgressDialog = ProgressDialog.show(this, getString(R.string.please_wait), getString(R.string.loading_forecast), true);
    }

    private void setNotLoadingState(){
        mLoadingData = false;
        mProgressDialog.cancel();
    }


    /* ------------------------------------------------------------------------------------------------
     * Getters and Setters
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
