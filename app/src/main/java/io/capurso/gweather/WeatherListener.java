package io.capurso.gweather;

import java.util.ArrayList;

import io.capurso.gweather.forecast.ForecastInfo;

/**
 * Created by Nick on 10/3/2015.
 */
public interface WeatherListener {
    void onWeatherReceived(ArrayList<ForecastInfo> forecast);
    void onWeatherError(byte errorCode);
}
