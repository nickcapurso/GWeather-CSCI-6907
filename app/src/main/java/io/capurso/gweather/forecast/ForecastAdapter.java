package io.capurso.gweather.forecast;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import java.util.List;

import io.capurso.gweather.R;

/**
 * Created by cheng on 9/26/15.
 */
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
        holder.setCurrTemp(info.currTemp);
        holder.setIcon(info.iconId);

        if(position > mLastAnimated) {
            holder.getContainer().startAnimation(AnimationUtils.loadAnimation(mContext, android.R.anim.fade_in));
            mLastAnimated = position;
        }
    }

    @Override
    public int getItemCount() {
        return mForecastInfo.size();
    }
}
