package io.capurso.gweather.common;

public class Utils {
    /**
     * Sets Log statements on/off.
     */
    public static final boolean DEBUG = true;

    /**
     * Used with startActivityForResult when starting the location services activity in the system settings.
     */
    public static final byte CODE_ENABLE_LOCATION = 0x01;

    /**
     * Used with startActivityForResult (a return code). Indicates that the forecast should be
     * refreshed to reflect changes in the user's settings.
     */
    public static final byte CODE_REFRESH_FORECAST = 0x02;
}
