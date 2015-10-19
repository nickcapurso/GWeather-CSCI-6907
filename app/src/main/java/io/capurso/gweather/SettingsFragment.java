package io.capurso.gweather;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

import static io.capurso.gweather.common.Utils.DEBUG;

/**
 * Recommended by the Android Developer's guide for handling user settings.
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener{
    private static final String TAG = SettingsFragment.class.getName();

    /**
     * Used to notify the SettingsActivity of changes to the preferences.
     */
    public interface onSettingsChangedListener{
        void onSettingChanged(boolean requiresRefresh);
    }

    /**
     * Used to send callbacks to the SettingsActivity.
     */
    private onSettingsChangedListener mListener;

    public SettingsFragment(){}

    /**
     * Takes in a listener for callbacks when the settings are changed.
     * @param listener
     */
    public SettingsFragment(onSettingsChangedListener listener){
        mListener = listener;
    }

    /**
     * Inflate and display the preferences resource file.
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        refreshCustomSummaries();
    }

    /**
     * Customize some of the preference summaries (for example, to display the zipcode that the
     * user had previously typed in).
     */
    private void refreshCustomSummaries(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        int forecastSize = prefs.getInt(getString(R.string.key_forecast_size), 3);
        setPreferenceSummary(R.string.key_forecast_size, forecastSize + getString(R.string.pref_forecast_summary_suffix));

        String zipcode = prefs.getString(getString(R.string.key_zipcode_set), "");
        setPreferenceSummary(R.string.key_zipcode_set, zipcode);
    }

    /**
     * Register the callback for changed preferences.
     */
    @Override
    public void onResume(){
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(this);
    }

    /**
     * Unregister the callback for changed preferences.
     */
    @Override
    public void onPause(){
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Update custom summaries based on which preference was changed.
     * @param sharedPreferences SharedPrefs reference
     * @param s The key of the preference which was changed.
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if(DEBUG) Log.d(TAG, s);

        mListener.onSettingChanged(!s.equals(getString(R.string.key_location_mode)));

        if(s.equals(getString(R.string.key_zipcode_use))) {
            findPreference(getString(R.string.key_location_mode))
                    .setEnabled(!sharedPreferences.getBoolean(s, false));
        }else if(s.equals(getString(R.string.key_forecast_size))){
            refreshCustomSummaries();
        }else if(s.equals(getString(R.string.key_zipcode_set))){
            refreshCustomSummaries();
        }
    }

    /**
     * Set the summary of the given preference to the passed String.
     * @param keyId ID of the preference whose summary to change.
     * @param summary The new summary
     */
    private void setPreferenceSummary(int keyId, String summary){
        findPreference(getString(keyId)).setSummary(summary);
    }
}
