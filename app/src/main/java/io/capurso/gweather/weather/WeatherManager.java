package io.capurso.gweather.weather;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import io.capurso.gweather.R;
import io.capurso.gweather.common.API_URLS;
import io.capurso.gweather.forecast.ForecastInfo;
import io.capurso.gweather.json.JSONEventListener;
import io.capurso.gweather.json.JSONFetcher;

import static io.capurso.gweather.common.Utils.DEBUG;

/**
 * Created by Nick on 10/3/2015.
 */
public class WeatherManager implements JSONEventListener{
    private static final String TAG = WeatherManager.class.getName();
    private ArrayList<ForecastInfo> mForecastInfos;

    private WeatherListener mClientListener;
    private Location mLocation;

    private String mTempMetric;
    private boolean mUseFahrenheit;
    private int mDaysToShow;

    public WeatherManager(WeatherListener clientListener, Location location, Context context){
        mClientListener = clientListener;
        mLocation = location;
        mForecastInfos = new ArrayList<ForecastInfo>();
        parseWeatherPreferences(context);
    }

    private void parseWeatherPreferences(Context context){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        Resources res = context.getResources();


        String tempPref = sharedPrefs.getString(res.getString(R.string.key_metric), res.getString(R.string.pref_metrics_list_default));
        mUseFahrenheit = tempPref.equals("0");
        mTempMetric = mUseFahrenheit ? res.getString(R.string.fahrenheit_symbol) : res.getString(R.string.celsius_symbol);
        mDaysToShow = sharedPrefs.getInt(res.getString(R.string.key_forecast_size), res.getInteger(R.integer.forecast_size_default));
    }

    public void requestForecast(){
        //TODO use something mutable
        String url = API_URLS.WUNDERGROUND + API_URLS.WUNDERGROUND_FORECAST;
        url += mLocation.getLatitude() + "," + mLocation.getLongitude();
        url += API_URLS.WUNDERGROUND_FORMAT;

        new JSONFetcher(this).execute(url);
    }

    private ForecastInfo createForecastInfo(JSONObject json)  {
        String day, weatherDesc, lowHigh, currTemp, iconUrl;

        try {
            day = json.getJSONObject("date").getString("weekday");
            weatherDesc = json.getString("conditions");

            JSONObject tempLow = json.getJSONObject("low");
            JSONObject tempHigh = json.getJSONObject("high");

            if(mUseFahrenheit) {
                lowHigh = tempLow.getString("fahrenheit") + mTempMetric +
                        " / " + tempHigh.getString("fahrenheit") + mTempMetric;

                //TODO need to use conditions API
                currTemp = tempLow.getString("fahrenheit") + mTempMetric;
            }else{
                lowHigh = tempLow.getString("celsius") + mTempMetric +
                        " / " + tempHigh.getString("celsius") + mTempMetric;

                //TODO need to use conditions API
                currTemp = tempLow.getString("celsius") + mTempMetric;
            }

            iconUrl = json.getString("icon_url");

            if(DEBUG) Log.d(TAG, "Day = " + day + ", weather = " + weatherDesc + ", lowhigh = " + lowHigh + ", iconUrl = " + iconUrl);
            return new ForecastInfo(day, weatherDesc, lowHigh, currTemp, iconUrl);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void parseJsonForecast(String jsonString){
        try {
            JSONObject result = new JSONObject(jsonString);
            JSONObject forecastTop = result.getJSONObject("forecast");

            JSONArray forecastArr = forecastTop.getJSONObject("simpleforecast").getJSONArray("forecastday");

            if(DEBUG) Log.d(TAG, "Forecasts available: " + forecastArr.length());

            for(int i = 0; i < forecastArr.length() && i < mDaysToShow; i++)
                mForecastInfos.add(createForecastInfo(forecastArr.getJSONObject(i)));

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onNetworkTimeout() {

    }

    @Override
    public void onJSONFetchErr() {

    }

    @Override
    public void onJSONFetchSuccess(String result) {
        parseJsonForecast(result);
        mClientListener.onWeatherReceived(mForecastInfos);
    }

    class ErrorCodes{
        static final byte ERR_NETWORK_TIMEOUT = 0x00;
        static final byte ERR_JSON_FAILED = 0x01;
    }
}
