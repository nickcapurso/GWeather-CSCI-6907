package io.capurso.gweather.forecast;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import io.capurso.gweather.R;

/**
 * Created by cheng on 9/26/15.
 */
public class ForecastAdapter extends RecyclerView.Adapter<ForecastViewHolder> {
    private List<ForecastInfo> mForecastInfo;


    public ForecastAdapter(List<ForecastInfo> info){
        mForecastInfo = info;
    }

    @Override
    public ForecastViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_forecast, parent, false);

        return new ForecastViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ForecastViewHolder holder, int position) {
        ForecastInfo info = mForecastInfo.get(position);
        holder.setDay(info.day);
        holder.setWeatherDesc(info.weatherDesc);
        holder.setLowHigh(info.lowHigh);
        holder.setCurrTemp(info.currTemp);
        holder.setIcon(info.iconId);
    }

    @Override
    public int getItemCount() {
        return mForecastInfo.size();
    }
}
