package io.capurso.gweather.json;

/**
 * Created by cheng on 9/26/15.
 */
public interface JSONEventListener {
    public void onNetworkTimeout();
    public void onJSONFetchErr();
    public void onJSONFetchSuccess(String result);
}
