package io.capurso.gweather.json;

/**
 * Listen for callbacks from the JSONFetcher class.
 */
public interface JSONEventListener {
    void onNetworkTimeout();
    void onJSONFetchErr();
    void onJSONFetchSuccess(String result);
}
