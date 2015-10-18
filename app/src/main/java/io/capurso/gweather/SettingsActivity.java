package io.capurso.gweather;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import io.capurso.gweather.common.Utils;


public class SettingsActivity extends AppCompatActivity implements SettingsFragment.onSettingsChangedListener{
    private static final String TAG = SettingsActivity.class.getName();

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setupActionBar();

        getFragmentManager().beginTransaction()
                .add(R.id.topLayout, new SettingsFragment(this))
                .commit();
    }

    private void setupActionBar(){
        ActionBar bar;
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.action_settings));
        setSupportActionBar(toolbar);

        if((bar = getSupportActionBar()) != null){
            bar.setHomeButtonEnabled(true);
            bar.setDisplayHomeAsUpEnabled(true);

            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }

    @Override
    public void onSettingChanged(boolean requiresRefresh) {
        if(requiresRefresh)
            setResult(Utils.CODE_REFRESH_FORECAST);
    }
}
