package io.capurso.gweather.json;

import android.os.CountDownTimer;

/**
 * Simple countdown timer for location and network requests. By default, the period is ten seconds.
 * When the timeout occurs, a message is sent to the "client" activity's handler.
 */
public class NetworkTimeout extends CountDownTimer {
    /**
     * One second
     */
    public static final long ONE_SECOND_MILLIS = 1000;

    /**
     * 15 * one second = 15 seconds
     */
    public static final long DEFAULT_PERIOD = 15 * ONE_SECOND_MILLIS;

    /**
     * Listener to receive a callback if the timeout expires
     */
    private JSONEventListener mClientListener;

    public NetworkTimeout(long millisInFuture, long countDownInterval, JSONEventListener listener){
        super(millisInFuture, countDownInterval);
        mClientListener = listener;
    }

    public NetworkTimeout(JSONEventListener listener){
        super(DEFAULT_PERIOD, ONE_SECOND_MILLIS);
        mClientListener = listener;
    }

    @Override
    public void onTick(long millisUntilFinished) { }

    /**
     * Send a timeout message to the client activity's handler when the timer finishes
     */
    @Override
    public void onFinish() {
        mClientListener.onNetworkTimeout();
    }
}
