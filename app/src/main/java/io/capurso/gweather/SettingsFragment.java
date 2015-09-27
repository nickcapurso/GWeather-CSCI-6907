package io.capurso.gweather;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import static io.capurso.gweather.common.Utils.DEBUG;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener{
    private static final String TAG = SettingsFragment.class.getName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        refreshCustomSummaries();
    }

    private void refreshCustomSummaries(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        int forecastSize = prefs.getInt(getString(R.string.key_forecast_size), 1);
        setPreferenceSummary(R.string.key_forecast_size, forecastSize + getString(R.string.pref_forecast_summary_suffix));

        String zipcode = prefs.getString(getString(R.string.key_zipcode_set), "");
        setPreferenceSummary(R.string.key_zipcode_set, zipcode);
    }

    @Override
    public void onResume(){
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause(){
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if(DEBUG) Log.d(TAG, s);

        if(s.equals(getString(R.string.key_zipcode_use))) {
            findPreference(getString(R.string.key_location_mode))
                    .setEnabled(!sharedPreferences.getBoolean(s, false));

        }else if(s.equals(getString(R.string.key_forecast_size))
                || s.equals(getString(R.string.key_zipcode_set))) {
            refreshCustomSummaries();
        }
    }

    private void setPreferenceSummary(int keyId, String summary){
        findPreference(getString(keyId)).setSummary(summary);
    }

    private void setPreferenceSummary(String key, String summary){
        findPreference(key).setSummary(summary);
    }
}
