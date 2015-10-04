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
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.capurso.gweather.forecast.ForecastAdapter;
import io.capurso.gweather.forecast.ForecastInfo;


public class ForecastActivity extends AppCompatActivity implements BlackboxListener, WeatherListener {
    private static final String TAG = ForecastActivity.class.getName();
    private static final long FORECAST_VIEW_DELAY = 300; //300 ms

    private LinearLayout mMainLayout;
    private RecyclerView mRvForecast;
    private RecyclerView.Adapter mAdapter;

    private WeatherManager mWeatherManager;
    private List<ForecastInfo> mForecastInfo;

    private Handler mHandler;

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
    }



    @Override
    public void onWeatherReceived(List<ForecastInfo> forecast) {
        long uptime = SystemClock.uptimeMillis();

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
    public void onWeatherError(byte errorCode) {

    }




    @Override
    public void onLocationFound(LocationWrapper location) {
        mWeatherManager = new WeatherManager(this, location.location);
        mWeatherManager.requestForecast();

        ((TextView)findViewById(R.id.tvLocationName)).setText(location.address);
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
                //TODO string constants
                Snackbar retryBar = Snackbar.make(mMainLayout, "Please check your internet connection", Snackbar.LENGTH_LONG);
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
                String provider = errCode == LocationBlackbox.ErrorCodes.ERR_GPS_DISABLED ? "GPS" : "Network";

                //TODO string constants
                Snackbar locationBar = Snackbar.make(mMainLayout, provider + " location is not enabled", Snackbar.LENGTH_LONG);
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
         /*   mForecastInfo.add( new ForecastInfo(
                    getResources().getString(R.string.placeholder_day),
                    getResources().getString(R.string.placeholder_weather),
                    getResources().getString(R.string.placeholder_lowhigh),
                    getResources().getString(R.string.placeholder_current),
                    R.drawable.ic_launcher
            ));
            mAdapter.notifyDataSetChanged();
            */
            new LocationBlackbox(this, this).requestLocation();
            return true;
        }else if(id == R.id.action_settings){
            startActivity(new Intent(this, SettingsActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }
}
