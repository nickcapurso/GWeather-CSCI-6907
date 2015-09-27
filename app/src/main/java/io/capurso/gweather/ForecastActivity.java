package io.capurso.gweather;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import io.capurso.gweather.forecast.ForecastAdapter;
import io.capurso.gweather.forecast.ForecastInfo;


public class ForecastActivity extends AppCompatActivity {
    private static final String TAG = ForecastActivity.class.getName();

    private RecyclerView mRvForecast;
    private RecyclerView.Adapter mAdapter;

    private List<ForecastInfo> mForecastInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecast);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        mForecastInfo = new ArrayList<ForecastInfo>();

        mRvForecast = (RecyclerView) findViewById(R.id.rvForecast);
        mRvForecast.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
            
        }else {
            layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        }

        mRvForecast.setLayoutManager(layoutManager);

        mAdapter = new ForecastAdapter(this, mForecastInfo);
        mRvForecast.setAdapter(mAdapter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_forecast, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            mForecastInfo.add( new ForecastInfo(
                    getResources().getString(R.string.placeholder_day),
                    getResources().getString(R.string.placeholder_weather),
                    getResources().getString(R.string.placeholder_lowhigh),
                    getResources().getString(R.string.placeholder_current),
                    R.drawable.ic_launcher
            ));
            mAdapter.notifyDataSetChanged();
            return true;
        }else if(id == R.id.action_settings){
            startActivity(new Intent(this, SettingsActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }
}
