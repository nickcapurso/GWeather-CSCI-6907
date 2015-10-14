package io.capurso.gweather.weather.forecast;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import java.util.List;

import io.capurso.gweather.R;

public class ForecastAdapter extends RecyclerView.Adapter<ForecastViewHolder> {
    private List<ForecastInfo> mForecastInfo;
    private Context mContext;

    private int mLastAnimated = -1;

    public ForecastAdapter(Context context, List<ForecastInfo> info){
        mContext = context;
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
        holder.setIcon(info.iconUrl, mContext);

        if(position > mLastAnimated) {
            holder.getContainer().startAnimation(AnimationUtils.loadAnimation(mContext, android.R.anim.slide_in_left));
            mLastAnimated = position;
        }
    }

    public void resetAnimationCount(){
        mLastAnimated = -1;
    }

    @Override
    public int getItemCount() {
        return mForecastInfo.size();
    }
}
