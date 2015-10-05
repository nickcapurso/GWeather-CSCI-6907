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

import io.capurso.gweather.forecast.ForecastAdapter;
import io.capurso.gweather.forecast.ForecastInfo;

public class ForecastActivity extends AppCompatActivity implements BlackboxListener, WeatherListener {
    private static final String TAG = ForecastActivity.class.getName();
    private static final long FORECAST_VIEW_DELAY = 300; //300 ms

    private static final String BUNDLE_KEY_FORECAST_LIST = "forecastList";
    private static final String BUNDLE_KEY_LOCATION_TITLE = "locationTitle";

    private LinearLayout mMainLayout;
    private RecyclerView mRvForecast;
    private ForecastAdapter mAdapter;

    private WeatherManager mWeatherManager;
    private ArrayList<ForecastInfo> mForecastInfo;

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

        if(savedInstanceState != null){
            ArrayList<ForecastInfo> temp = savedInstanceState.getParcelableArrayList(BUNDLE_KEY_FORECAST_LIST);

            if(temp == null)
                return;

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
        super.onSaveInstanceState(toSave);
    }


    @Override
    public void onWeatherReceived(ArrayList<ForecastInfo> forecast) {
        staggerInForecast(forecast);
    }

    @Override
    public void onWeatherError(byte errorCode) {

    }

    private void staggerInForecast(ArrayList<ForecastInfo> forecast){
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
    public void onLocationFound(LocationWrapper location) {
        mWeatherManager = new WeatherManager(this, location.location, this);
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
/*
            new JSONFetcher(new JSONEventListener() {
                @Override
                public void onNetworkTimeout() {

                }

                @Override
                public void onJSONFetchErr() {

                }

                @Override
                public void onJSONFetchSuccess(String result) {
                    try {
                        JSONObject topObj = new JSONObject(result);
                        JSONArray imageArr = topObj.getJSONObject("responseData").getJSONArray("results");
                        String imgUrl = imageArr.getJSONObject(0).getString("url");

                        if(DEBUG) Log.d(TAG, "Image: " + imgUrl);

                        ImageView banner = (ImageView)findViewById(R.id.ivLocationBanner);
                        Picasso.with(ForecastActivity.this).load(imgUrl).into(banner);
                        banner.setVisibility(View.VISIBLE);
                        ViewGroup.LayoutParams params = banner.getLayoutParams();
                        params.height = (int)getResources().getDimension(R.dimen.banner_height);
                        banner.setLayoutParams(params);


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }).execute(API_URLS.GOOGLE_IMAGES, API_URLS.GOOGLE_IMAGES_QUERY, "washingtondc",
                    API_URLS.GOOGLE_IMAGES_RESPONSES, "2");

*/
            return true;
        }else if(id == R.id.action_settings){
            startActivity(new Intent(this, SettingsActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }


}
