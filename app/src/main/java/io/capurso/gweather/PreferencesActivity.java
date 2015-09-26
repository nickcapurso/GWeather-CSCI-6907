package io.capurso.gweather;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Created by cheng on 9/26/15.
 */
public class PreferencesActivity  extends PreferenceActivity{

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
