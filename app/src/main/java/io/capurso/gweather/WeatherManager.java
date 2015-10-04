package io.capurso.gweather;

import android.location.Location;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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
    private List<ForecastInfo> mForecastInfos;

    private WeatherListener mClientListener;
    private Location mLocation;

    public WeatherManager(WeatherListener clientListener, Location location){
        mClientListener = clientListener;
        mLocation = location;
        mForecastInfos = new ArrayList<ForecastInfo>();
    }

    public void requestForecast(){
        //TODO use something mutable
        String url = API_URLS.WUNDERGROUND + API_URLS.WUNDERGROUND_FORECAST;
        url += mLocation.getLatitude() + "," + mLocation.getLongitude();
        url += API_URLS.WUNDERGROUND_FORMAT;

        new JSONFetcher(this).execute(url);
    }

    private ForecastInfo createForecastInfo(JSONObject json)  {
        String day, weatherDesc, lowHigh, currTemp;

        try {
            day = json.getJSONObject("date").getString("weekday");
            weatherDesc = json.getString("conditions");

            JSONObject tempLow = json.getJSONObject("low");
            JSONObject tempHigh = json.getJSONObject("high");

            //TODO celsius
            if(true) {
                //TODO degrees symbol
                lowHigh = tempLow.getString("fahrenheit") + " / " + tempHigh.getString("fahrenheit");

                //TODO need to use conditions API
                currTemp = tempLow.getString("fahrenheit");
            }

            if(DEBUG) Log.d(TAG, "Day = " + day + ", weather = " + weatherDesc + ", lowhigh = " + lowHigh + ", currTemp = " + currTemp);
            return new ForecastInfo(day, weatherDesc, lowHigh, currTemp, R.drawable.ic_launcher);
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

            for(int i = 0; i < forecastArr.length(); i++)
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
