package io.capurso.gweather.common;

/**
 * The URLs and their parameters used by the application.
 */
public class API_URLS {
    public static final String GEOCODING = "http://maps.google.com/maps/api/geocode/json";
    public static final String GEOCODING_ADDR_PARAM = "address";

    public static final String WUNDERGROUND = "http://api.wunderground.com/api/6a78e53086342a73";
    public static final String WUNDERGROUND_FORMAT = ".json";
    public static final String WUNDERGROUND_FORECAST = "/forecast10day/q/";
    public static final String WUNDERGROUND_CONDITIONS = "/conditions/q/";
    public static final String WUNDERGROUND_REVERSE_GEOCODE = "/geolookup/q/";

    public static final String GOOGLE_IMAGES = "https://ajax.googleapis.com/ajax/services/search/images?v=1.0";
    public static final String GOOGLE_IMAGES_QUERY = "q";
    public static final String GOOGLE_IMAGES_RESPONSES = "rsz";

}
