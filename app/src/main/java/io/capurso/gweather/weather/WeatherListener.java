package io.capurso.gweather.weather;

import java.util.ArrayList;

import io.capurso.gweather.weather.forecast.ForecastInfo;

/**
 * Created by Nick on 10/3/2015.
 */
public interface WeatherListener {
    void onWeatherReceived(ArrayList<ForecastInfo> forecast);
    void onWeatherError(byte errorCode);
}
