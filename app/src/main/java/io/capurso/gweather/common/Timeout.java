package io.capurso.gweather.common;

import android.os.CountDownTimer;

/**
 * Simple countdown timer for location and network requests. By default, the period is ten seconds.
 * When the timeout occurs, the client activity is notified over an interface.
 */
public class Timeout extends CountDownTimer {
    /**
     * One second.
     */
    public static final long ONE_SECOND_MILLIS = 1000;

    /**
     * 10 * one second = 10 seconds.
     */
    public static final long DEFAULT_PERIOD = 10 * ONE_SECOND_MILLIS;

    /**
     * Listener to receive a callback if the timeout expires.
     */
    private TimeoutListener mClientListener;

    /**
     * Constructor used to set a time and tick intervals.
     * @param millisInFuture Time in the future from when start() is called
     * @param countDownInterval Tick interval
     * @param listener For timeout callbacks
     */
    public Timeout(long millisInFuture, long countDownInterval, TimeoutListener listener){
        super(millisInFuture, countDownInterval);
        mClientListener = listener;
    }

    /**
     * Takes in only a listener - sets the length to the default 10 seconds and the
     * tick interval to one second.
     * @param listener For timeout callbacks
     */
    public Timeout(TimeoutListener listener){
        super(DEFAULT_PERIOD, ONE_SECOND_MILLIS);
        mClientListener = listener;
    }

    /**
     * Unused
     */
    @Override
    public void onTick(long millisUntilFinished) { }

    /**
     * Notify the client that the timeout has expired
     */
    @Override
    public void onFinish() {
        mClientListener.onTimeout();
    }
}
