package io.capurso.gweather.location;

/**
 * Listener for location-related events by the LocationBlackbox class.
 */
public interface BlackboxListener {
    void onLocationFound(LocationWrapper location);
    void onBlackboxError(byte errCode);
}
