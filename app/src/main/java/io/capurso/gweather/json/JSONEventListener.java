package io.capurso.gweather.json;

/**
 * Listen for callbacks from the JSONFetcher class.
 */
public interface JSONEventListener {
    public void onNetworkTimeout();
    public void onJSONFetchErr();
    public void onJSONFetchSuccess(String result);
}
