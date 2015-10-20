package io.capurso.gweather.weather;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import io.capurso.gweather.R;
import io.capurso.gweather.common.API_URLS;
import io.capurso.gweather.json.JSONEventListener;
import io.capurso.gweather.json.JSONFetcher;
import io.capurso.gweather.weather.forecast.ForecastInfo;

import static io.capurso.gweather.common.Utils.DEBUG;

/**
 * Manages the task of retrieving weather data from Wunderground.
 */
public class WeatherManager implements JSONEventListener{
    private static final String TAG = WeatherManager.class.getName();

    //Used to determine how to interpret the received JSON resposne
    private static final byte STATE_GET_FORECAST = 0x00;
    private static final byte STATE_GET_CONDITIONS = 0x01;

    /**
     * List of forecast information that will eventually be returned.
     */
    private ArrayList<ForecastInfo> mForecastInfos;

    /**
     * Listener for weather-related callbacks.
     */
    private WeatherListener mClientListener;

    /**
     * The current location. Used to get weather where the user currently is.
     */
    private Location mLocation;

    //Strings that correspond to the user's current measurement system (ex. "cm" vs "in", "Fahrenheit" vs "Celcius", etc.
    private String mTemperatureMetric, mHeightMetric, mHeightMetric2, mSpeedMetric, mAbbrTemp, mCurrTemp;

    /**
     * Which measurement system is the user currently using?
     */
    private boolean mUseImperialSystem;

    /**
     * The number of days to forecast.
     */
    private int mDaysToShow;

    /**
     * Are we currently getting the forecast or the current weather conditions?
     */
    private byte mState;

    /**
     * Used to make network requests to the Wunderground API.
     */
    private JSONFetcher mJSONFetcher;

    /**
     * @param clientListener Listener for weather callbacks.
     * @param location The user's current location for accurate weather.
     * @param context Context, for parsing the user's preferences.
     */
    public WeatherManager(WeatherListener clientListener, Location location, Context context){
        mClientListener = clientListener;
        mLocation = location;
        mForecastInfos = new ArrayList<ForecastInfo>();
        parseWeatherPreferences(context);
    }

    /**
     * Read the preferences file and determine whether the user is on the imperial or metric system.
     * Also determine the amount of days to forecast. See: Metrics class at the bottom of this file.
     * @param context
     */
    private void parseWeatherPreferences(Context context){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        Resources res = context.getResources();

        String tempPref = sharedPrefs.getString(res.getString(R.string.key_metric), res.getString(R.string.pref_metrics_list_default));
        mUseImperialSystem = tempPref.equals("0");
        mTemperatureMetric = getUnit(mUseImperialSystem, Metrics.TYPE_TEMPERATURE);
        mAbbrTemp = mUseImperialSystem ? res.getString(R.string.fahrenheit_symbol) : res.getString(R.string.celsius_symbol);
        mHeightMetric = getUnit(mUseImperialSystem, Metrics.TYPE_HEIGHT);
        mHeightMetric2 = getUnit(mUseImperialSystem, Metrics.TYPE_HEIGHT_SECONDARY);
        mSpeedMetric = getUnit(mUseImperialSystem, Metrics.TYPE_SPEED);
        mDaysToShow = sharedPrefs.getInt(res.getString(R.string.key_forecast_size), res.getInteger(R.integer.forecast_size_default));
    }

    /**
     * Start a network request to the Wunderground API to get the weekly forecast.
     */
    public void requestForecast(){
        //Set up forecast URL
        StringBuilder url = new StringBuilder(API_URLS.WUNDERGROUND).append(API_URLS.WUNDERGROUND_FORECAST)
        .append(mLocation.getLatitude()).append(",").append(mLocation.getLongitude())
        .append(API_URLS.WUNDERGROUND_FORMAT);

        mState = STATE_GET_FORECAST;
        (mJSONFetcher = new JSONFetcher(this)).execute(url.toString());
    }

    /**
     * Start a network request to the Wunderground API to get the current weather conditions (including temp).
     */
    public void requestCurrentTemp(){
        //Set up current weather conditions URL
        StringBuilder url = new StringBuilder(API_URLS.WUNDERGROUND).append(API_URLS.WUNDERGROUND_CONDITIONS)
        .append(mLocation.getLatitude()).append(",").append(mLocation.getLongitude())
        .append(API_URLS.WUNDERGROUND_FORMAT);

        mState = STATE_GET_CONDITIONS;
        (mJSONFetcher = new JSONFetcher(this)).execute(url.toString());
    }

    /**
     * Cancel any ongoing request (ex. before screen rotation)
     */
    public void cancel(){
        if(mJSONFetcher != null)
            mJSONFetcher.cancel(true);
    }

    /**
     * Given a JSON response representing a day's forecast, create the corresponding
     * ForecastInfo object.
     * @param simpleForecast The part of the response containing the weather data.
     * @param txtForecast The part of the response containing textual descriptions of the weather.
     * @return
     */
    private ForecastInfo createForecastInfo(JSONObject simpleForecast, JSONObject txtForecast)  {
        ForecastInfo info = new ForecastInfo();

        try {
            info.day = simpleForecast.getJSONObject("date").getString("weekday");
            info.weatherDesc = simpleForecast.getString("conditions");

            JSONObject tempLow = simpleForecast.getJSONObject("low");
            JSONObject tempHigh = simpleForecast.getJSONObject("high");

            info.lowHigh = tempLow.getString(mTemperatureMetric) + " " + mAbbrTemp +
                    " / " + tempHigh.getString(mTemperatureMetric) + " " + mAbbrTemp;
            info.iconUrl = simpleForecast.getString("icon_url");
            info.rainIn = simpleForecast.getJSONObject("qpf_allday").getDouble(mHeightMetric);
            info.snowIn = simpleForecast.getJSONObject("snow_allday").getDouble(mHeightMetric2);
            info.windDir = simpleForecast.getJSONObject("avewind").getString("dir");
            info.aveWind = simpleForecast.getJSONObject("avewind").getInt(mSpeedMetric);
            info.maxWind = simpleForecast.getJSONObject("maxwind").getInt(mSpeedMetric);
            info.humidity = simpleForecast.getInt("avehumidity");

            info.formalDesc = mUseImperialSystem ? txtForecast.getString("fcttext") : txtForecast.getString("fcttext_metric");

            if(DEBUG) Log.d(TAG, "" + info);
            return info;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //TODO, unlikely to happen unless JSON response from API changes
        return null;
    }

    /**
     * Given a JSON response representing a weekly forecast, create a ForecastInfo
     * object for each day.
     * @param jsonString
     */
    private void parseJsonForecast(String jsonString){
        try {
            JSONObject result = new JSONObject(jsonString);
            JSONObject forecastTop = result.getJSONObject("forecast");

            JSONArray forecastArr = forecastTop.getJSONObject("simpleforecast").getJSONArray("forecastday");
            JSONArray textArr = forecastTop.getJSONObject("txt_forecast").getJSONArray("forecastday");

            if(DEBUG) Log.d(TAG, "Forecasts available: " + forecastArr.length());

            //Create a ForecastInfo object for each day and add it to the list
            for(int i = 0; i < forecastArr.length() && i < mDaysToShow; i++)
                mForecastInfos.add(createForecastInfo(forecastArr.getJSONObject(i), textArr.getJSONObject(i)));

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Given a JSON response containing the current weather conditions, parse out the current temperature.
     * @param jsonString
     */
    private void parseCurrentConditions(String jsonString){
        try {
            JSONObject result = new JSONObject(jsonString);
            JSONObject conditionsTop = result.getJSONObject("current_observation");

            mCurrTemp = "" + (mUseImperialSystem ? conditionsTop.getInt("temp_f") : conditionsTop.getInt("temp_c"));
            mCurrTemp += " " + mAbbrTemp;
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the current temperature.
     * @return
     */
    public String getCurrTemp(){
        return mCurrTemp;
    }

    /**
     * Get the forecast list
     * @param forecast
     */
    public void setForecastInfos(ArrayList<ForecastInfo> forecast){
        mForecastInfos = forecast;
    }

    /**
     * Returns a dialog containing detailed information for the indicated day (corresponding
     * to a ForecastInfo object).
     * @param index Index into the forecast list
     * @param context
     * @return
     */
    public AlertDialog getDetailDialog(int index, Activity context){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = context.getLayoutInflater();

        //Inflate the dialog layout
        View layout = inflater.inflate(R.layout.view_forecast_detail, null);

        ForecastInfo info = mForecastInfos.get(index);
        ImageView icon = (ImageView) layout.findViewById(R.id.detail_icon);

        //Concatenate the day with the weather condition ("Saturday - Clear")
        String dayCondition = info.day + " - " + info.weatherDesc;

        //Separate the low and high temp ("57 F / 75 F" => "57 F", "75 F")
        String low = info.lowHigh.substring(0, info.lowHigh.indexOf('/')-1);
        String high = info.lowHigh.substring(info.lowHigh.indexOf('/') + 1);

        //Set the text of all the detail TextViews
        ((TextView)layout.findViewById(R.id.detail_day_condition)).setText(dayCondition);
        ((TextView)layout.findViewById(R.id.value_detail_low)).setText(low);
        ((TextView)layout.findViewById(R.id.value_detail_high)).setText(high);
        ((TextView)layout.findViewById(R.id.value_detail_humidity)).setText("" + info.humidity + "%");
        ((TextView)layout.findViewById(R.id.detail_full_desc)).setText("" + info.formalDesc);
        ((TextView)layout.findViewById(R.id.value_detail_rain)).setText("" + info.rainIn + " " + mHeightMetric2);
        ((TextView)layout.findViewById(R.id.value_detail_avgwind)).setText("" + info.aveWind + " " + mSpeedMetric);
        ((TextView)layout.findViewById(R.id.value_detail_maxwind)).setText("" + info.maxWind + " " + mSpeedMetric);
        ((TextView)layout.findViewById(R.id.value_detail_winddir)).setText(info.windDir);
        ((TextView)layout.findViewById(R.id.value_detail_snow)).setText("" + info.snowIn + " " + mHeightMetric);

        //Set up the "Okay" button to dismiss the dialog
        builder.setPositiveButton(context.getResources().getString(R.string.okay), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setView(layout);

        //Load the weather icon
        Picasso.with(context).load(info.iconUrl).placeholder(R.drawable.placeholder).into(icon);

        AlertDialog dialog = builder.create();

        //Add a slide up/down animation
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        return dialog;
    }

    /**
     * Returns the corresponding unit of measurement (as a String) for a measurement system.
     * @param imperialSystem
     * @param type See the Metrics class.
     * @return Measurement unit
     */
    private static String getUnit(boolean imperialSystem, byte type){
        switch(type){
            case Metrics.TYPE_TEMPERATURE:
                return imperialSystem ? Metrics.TEMP_IMPERIAL : Metrics.TEMP_METRIC;
            case Metrics.TYPE_HEIGHT:
                return imperialSystem ? Metrics.HEIGHT_IMPERIAL : Metrics.HEIGHT_METRIC_2;
            case Metrics.TYPE_HEIGHT_SECONDARY:
                return imperialSystem ? Metrics.HEIGHT_IMPERIAL : Metrics.HEIGHT_METRIC;
            case Metrics.TYPE_SPEED:
                return imperialSystem ? Metrics.SPEED_IMPERIAL : Metrics.SPEED_METRIC;
        }
        return null;
    }

    /**
     * Notify the listener of a network error.
     */
    @Override
    public void onNetworkTimeout() {
        mClientListener.onWeatherError(ErrorCodes.ERR_NETWORK_TIMEOUT);
    }

    /**
     * Notify the listener of a network error.
     */
    @Override
    public void onJSONFetchErr() {
        mClientListener.onWeatherError(ErrorCodes.ERR_JSON_FAILED);
    }

    /**
     * Notify the listener when the weather has been successfully retrieved.
     */
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

    /**
     * Defines constants for different units of measurements.
     */
    private static class Metrics{
        private static final byte TYPE_TEMPERATURE = 0x00;
        private static final byte TYPE_HEIGHT = 0x01;
        private static final byte TYPE_HEIGHT_SECONDARY = 0x02;
        private static final byte TYPE_SPEED = 0x03;

        private static final String TEMP_IMPERIAL = "fahrenheit";
        private static final String TEMP_METRIC = "celsius";

        private static final String HEIGHT_IMPERIAL = "in";
        private static final String HEIGHT_METRIC = "cm";
        private static final String HEIGHT_METRIC_2 = "mm";

        private static final String SPEED_IMPERIAL = "mph";
        private static final String SPEED_METRIC = "kph";
    }

    /**
     * Defines error codes to notify the listener with in case of problems.
     */
    public static class ErrorCodes{
        public static final byte ERR_NETWORK_TIMEOUT = 0x00;
        public static final byte ERR_JSON_FAILED = 0x01;
    }
}
