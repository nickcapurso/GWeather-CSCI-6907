package io.capurso.gweather.weather;

import java.util.ArrayList;

import io.capurso.gweather.weather.forecast.ForecastInfo;

/**
 * Listener for weather-related events by the WeatherManager.
 */
public interface WeatherListener {
    void onForecastReceived(ArrayList<ForecastInfo> forecast);
    void onCurrentTempReceived(String temp);
    void onWeatherError(byte errorCode);
}
