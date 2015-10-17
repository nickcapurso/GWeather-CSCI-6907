package io.capurso.gweather;

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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import io.capurso.gweather.weather.forecast.ForecastAdapter;
import io.capurso.gweather.weather.forecast.ForecastInfo;
import io.capurso.gweather.location.BlackboxListener;
import io.capurso.gweather.location.LocationBlackbox;
import io.capurso.gweather.location.LocationWrapper;
import io.capurso.gweather.weather.WeatherListener;
import io.capurso.gweather.weather.WeatherManager;

public class ForecastActivity extends AppCompatActivity implements BlackboxListener, WeatherListener {
    private static final String TAG = ForecastActivity.class.getName();
    private static final long FORECAST_VIEW_DELAY = 300; //300 ms

    private static final String BUNDLE_KEY_FORECAST_LIST = "forecastList";
    private static final String BUNDLE_KEY_LOCATION_TITLE = "locationTitle";
    private static final String BUNDLE_KEY_LOCATION_WRAPPER = "locationWrapper";
    private static final String BUNDLE_KEY_CURR_TEMP = "currTemp";

    private LinearLayout mMainLayout;
    private RecyclerView mRvForecast;
    private ForecastAdapter mAdapter;

    private WeatherManager mWeatherManager;
    private BannerManager mBannerManager;
    private ArrayList<ForecastInfo> mForecastInfo;

    private Handler mHandler;
    private LocationWrapper mCurrLocation;
    private String mCurrTemp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecast);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        mMainLayout = (LinearLayout)findViewById(R.id.llForecast);
        mForecastInfo = new ArrayList<ForecastInfo>();

        mRvForecast = (RecyclerView) findViewById(R.id.rvForecast);
        mRvForecast.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        else
            layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        mRvForecast.setLayoutManager(layoutManager);

        mAdapter = new ForecastAdapter(this, mForecastInfo);
        mRvForecast.setAdapter(mAdapter);

        mHandler = new Handler();

        mBannerManager = new BannerManager(this, (ImageView)findViewById(R.id.ivLocationBanner), (TextView)findViewById(R.id.tvCurrentTemp));

        if(savedInstanceState != null){
            ArrayList<ForecastInfo> temp = savedInstanceState.getParcelableArrayList(BUNDLE_KEY_FORECAST_LIST);

            if(temp == null)
                return;

            mCurrLocation = (LocationWrapper)savedInstanceState.getParcelable(BUNDLE_KEY_LOCATION_WRAPPER);
            mCurrTemp = savedInstanceState.getString(BUNDLE_KEY_CURR_TEMP);

            if(getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                mBannerManager.setupBanner(mCurrLocation);
                mBannerManager.setCurrentTemp(mCurrTemp);
            }

            staggerInForecast(temp);
            ((TextView)findViewById(R.id.tvLocationName)).setText(
                    savedInstanceState.getString(BUNDLE_KEY_LOCATION_TITLE)
            );
        }
    }

    @Override
    public void onSaveInstanceState(Bundle toSave){
        if(mForecastInfo.size() == 0)
            return;
        String locationTitle = ((TextView)findViewById(R.id.tvLocationName)).getText().toString();

        toSave.putParcelableArrayList(BUNDLE_KEY_FORECAST_LIST, mForecastInfo);
        toSave.putString(BUNDLE_KEY_LOCATION_TITLE, locationTitle);
        toSave.putParcelable(BUNDLE_KEY_LOCATION_WRAPPER, mCurrLocation);
        toSave.putString(BUNDLE_KEY_CURR_TEMP, mCurrTemp);
        super.onSaveInstanceState(toSave);
    }


    @Override
    public void onForecastReceived(ArrayList<ForecastInfo> forecast) {
        staggerInForecast(forecast);
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
        switch (errorCode){
            case WeatherManager.ErrorCodes.ERR_JSON_FAILED:
            case WeatherManager.ErrorCodes.ERR_NETWORK_TIMEOUT:

                //TODO string constants
                String message = "Please check your internet connection";

                Snackbar retryBar = Snackbar.make(mMainLayout, message, Snackbar.LENGTH_LONG);
                retryBar.setAction("Retry", new View.OnClickListener() {
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
        switch (errCode){
            case LocationBlackbox.ErrorCodes.ERR_BAD_ZIP:
                //TODO string constants
                Snackbar fixBar = Snackbar.make(mMainLayout, "Invalid zipcode", Snackbar.LENGTH_LONG);
                fixBar.setAction("Fix", new View.OnClickListener() {
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
                //TODO string constants
                String message = errCode == LocationBlackbox.ErrorCodes.ERR_LOCATION_TIMEOUT?
                        "Could not determine location" : "Please check your internet connection";

                Snackbar retryBar = Snackbar.make(mMainLayout, message, Snackbar.LENGTH_LONG);
                retryBar.setAction("Retry", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        new LocationBlackbox(ForecastActivity.this, ForecastActivity.this).requestLocation();
                    }
                });

                retryBar.show();
                break;

            case LocationBlackbox.ErrorCodes.ERR_GPS_DISABLED:
            case LocationBlackbox.ErrorCodes.ERR_NETWORK_DISABLED:
            case LocationBlackbox.ErrorCodes.ERR_LOCATION_DISABLED:
                String provider;
                if(errCode != LocationBlackbox.ErrorCodes.ERR_LOCATION_DISABLED)
                    provider = errCode == LocationBlackbox.ErrorCodes.ERR_GPS_DISABLED ? "GPS location" : "Network location";
                else
                    provider = "Location services ";

                //TODO string constants
                Snackbar locationBar = Snackbar.make(mMainLayout, provider + " is not enabled", Snackbar.LENGTH_LONG);
                locationBar.setAction("Fix", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);

                        //TODO forResult
                        startActivity(intent);
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //Placeholder - refresh creates dummy data
        if (id == R.id.action_refresh) {
            mForecastInfo.clear();
            mAdapter.notifyDataSetChanged();
            new LocationBlackbox(this, this).requestLocation();
            mAdapter.resetAnimationCount();
            mBannerManager.hideCurrentTemp();
            return true;
        }else if(id == R.id.action_settings){
            startActivity(new Intent(this, SettingsActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }


}
