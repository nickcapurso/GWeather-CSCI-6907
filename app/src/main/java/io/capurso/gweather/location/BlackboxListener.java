package io.capurso.gweather.location;

/**
 * Created by Nick on 10/3/2015.
 */
interface BlackboxListener {
    void onLocationFound(LocationWrapper location);
    void onBlackboxError(byte errCode);
}
