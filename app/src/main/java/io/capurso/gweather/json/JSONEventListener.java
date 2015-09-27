package io.capurso.gweather.json;

public interface JSONEventListener {
    public void onNetworkTimeout();
    public void onJSONFetchErr();
    public void onJSONFetchSuccess(String result);
}
