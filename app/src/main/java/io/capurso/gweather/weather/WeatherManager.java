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
import io.capurso.gweather.weather.forecast.ForecastInfo;
import io.capurso.gweather.json.JSONEventListener;
import io.capurso.gweather.json.JSONFetcher;

import static io.capurso.gweather.common.Utils.DEBUG;

/**
 * Created by Nick on 10/3/2015.
 */
public class WeatherManager implements JSONEventListener{
    private static final String TAG = WeatherManager.class.getName();
    private static final byte STATE_GET_FORECAST = 0x00;
    private static final byte STATE_GET_CONDITIONS = 0x01;


    private ArrayList<ForecastInfo> mForecastInfos;

    private WeatherListener mClientListener;
    private Location mLocation;

    private String mTempMetric, mCurrTemp;
    private boolean mUseFahrenheit;
    private int mDaysToShow;

    private byte mState;

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

        mState = STATE_GET_FORECAST;
        new JSONFetcher(this).execute(url);
    }

    public void requestCurrentTemp(){
        //TODO use something mutable
        String url = API_URLS.WUNDERGROUND + API_URLS.WUNDERGROUND_CONDITIONS;
        url += mLocation.getLatitude() + "," + mLocation.getLongitude();
        url += API_URLS.WUNDERGROUND_FORMAT;

        mState = STATE_GET_CONDITIONS;
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
            }else{
                lowHigh = tempLow.getString("celsius") + mTempMetric +
                        " / " + tempHigh.getString("celsius") + mTempMetric;
            }

            iconUrl = json.getString("icon_url");

            if(DEBUG) Log.d(TAG, "Day = " + day + ", weather = " + weatherDesc + ", lowhigh = " + lowHigh + ", iconUrl = " + iconUrl);
            return new ForecastInfo(day, weatherDesc, lowHigh, iconUrl);
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

    private void parseCurrentConditions(String jsonString){
        try {
            JSONObject result = new JSONObject(jsonString);
            JSONObject conditionsTop = result.getJSONObject("current_observation");

            mCurrTemp = "" + (mUseFahrenheit ? conditionsTop.getDouble("temp_f") : conditionsTop.getDouble("temp_c"));
            mCurrTemp += mTempMetric;
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getCurrTemp(){
        return mCurrTemp;
    }

    @Override
    public void onNetworkTimeout() {
        mClientListener.onWeatherError(ErrorCodes.ERR_NETWORK_TIMEOUT);
    }

    @Override
    public void onJSONFetchErr() {
        mClientListener.onWeatherError(ErrorCodes.ERR_JSON_FAILED);
    }

    @Override
    public void onJSONFetchSuccess(String result) {
        switch(mState){
            case STATE_GET_FORECAST:
                parseJsonForecast(result);
                mClientListener.onForecastReceived(mForecastInfos);
                break;
            case STATE_GET_CONDITIONS:
                parseCurrentConditions(result);
                mClientListener.onCurrentTempReceived(mCurrTemp);
                break;
        }

    }

    public static class ErrorCodes{
        public static final byte ERR_NETWORK_TIMEOUT = 0x00;
        public static final byte ERR_JSON_FAILED = 0x01;
    }
}
