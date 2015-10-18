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
 * Created by Nick on 10/3/2015.
 */
public class WeatherManager implements JSONEventListener{
    private static final String TAG = WeatherManager.class.getName();
    private static final byte STATE_GET_FORECAST = 0x00;
    private static final byte STATE_GET_CONDITIONS = 0x01;

    private ArrayList<ForecastInfo> mForecastInfos;

    private WeatherListener mClientListener;
    private Location mLocation;

    private String mTemperatureMetric, mHeightMetric, mHeightMetric2, mSpeedMetric, mAbbrTemp, mCurrTemp;
    private boolean mUseImperialSystem;
    private int mDaysToShow;

    private byte mState;

    private JSONFetcher mJSONFetcher;

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
        mUseImperialSystem = tempPref.equals("0");
        mTemperatureMetric = getUnit(mUseImperialSystem, Metrics.TYPE_TEMPERATURE);
        mAbbrTemp = mUseImperialSystem ? res.getString(R.string.fahrenheit_symbol) : res.getString(R.string.celsius_symbol);
        mHeightMetric = getUnit(mUseImperialSystem, Metrics.TYPE_HEIGHT);
        mHeightMetric2 = getUnit(mUseImperialSystem, Metrics.TYPE_HEIGHT_SECONDARY);
        mSpeedMetric = getUnit(mUseImperialSystem, Metrics.TYPE_SPEED);
        mDaysToShow = sharedPrefs.getInt(res.getString(R.string.key_forecast_size), res.getInteger(R.integer.forecast_size_default));
    }

    public void requestForecast(){
        //TODO use something mutable
        String url = API_URLS.WUNDERGROUND + API_URLS.WUNDERGROUND_FORECAST;
        url += mLocation.getLatitude() + "," + mLocation.getLongitude();
        url += API_URLS.WUNDERGROUND_FORMAT;

        mState = STATE_GET_FORECAST;
        (mJSONFetcher = new JSONFetcher(this)).execute(url);;
    }

    public void requestCurrentTemp(){
        //TODO use something mutable
        String url = API_URLS.WUNDERGROUND + API_URLS.WUNDERGROUND_CONDITIONS;
        url += mLocation.getLatitude() + "," + mLocation.getLongitude();
        url += API_URLS.WUNDERGROUND_FORMAT;

        mState = STATE_GET_CONDITIONS;
        (mJSONFetcher = new JSONFetcher(this)).execute(url);;
    }

    public void cancel(){
        if(mJSONFetcher != null)
            mJSONFetcher.cancel(true);
    }

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

            Log.d(TAG, "" + info);
            return info;
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
            JSONArray textArr = forecastTop.getJSONObject("txt_forecast").getJSONArray("forecastday");

            if(DEBUG) Log.d(TAG, "Forecasts available: " + forecastArr.length());

            for(int i = 0; i < forecastArr.length() && i < mDaysToShow; i++)
                mForecastInfos.add(createForecastInfo(forecastArr.getJSONObject(i), textArr.getJSONObject(i)));

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

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

    public String getCurrTemp(){
        return mCurrTemp;
    }

    public void setForecastInfos(ArrayList<ForecastInfo> forecast){
        mForecastInfos = forecast;
    }

    public AlertDialog getDetailDialog(int index, Activity context){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = context.getLayoutInflater();

        View layout = inflater.inflate(R.layout.view_forecast_detail, null);
        ForecastInfo info = mForecastInfos.get(index);
        ImageView icon = (ImageView) layout.findViewById(R.id.detail_icon);

        String dayCondition = info.day + " - " + info.weatherDesc;
        String low = info.lowHigh.substring(0, info.lowHigh.indexOf('/')-1);
        String high = info.lowHigh.substring(info.lowHigh.indexOf('/') + 1);

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

        builder.setPositiveButton(context.getResources().getString(R.string.okay), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setView(layout);
        Picasso.with(context).load(info.iconUrl).placeholder(R.drawable.placeholder).into(icon);

        AlertDialog dialog = builder.create();
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        return dialog;
    }


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


    public static class ErrorCodes{
        public static final byte ERR_NETWORK_TIMEOUT = 0x00;
        public static final byte ERR_JSON_FAILED = 0x01;
    }
}
