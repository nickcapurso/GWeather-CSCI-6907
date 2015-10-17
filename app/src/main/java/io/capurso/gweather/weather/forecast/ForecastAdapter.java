package io.capurso.gweather.weather.forecast;

import android.content.Context;
import android.content.res.Configuration;
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
    private int mOrientation;
    private ForecastViewHolder.ForecastViewClickListener mListener;

    public ForecastAdapter(Context context, List<ForecastInfo> info, ForecastViewHolder.ForecastViewClickListener listener){
        mContext = context;
        mForecastInfo = info;
        mOrientation = context.getResources().getConfiguration().orientation;
        mListener = listener;
    }

    @Override
    public ForecastViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_forecast, parent, false);

        return new ForecastViewHolder(v, mOrientation, mListener);
    }

    @Override
    public void onBindViewHolder(ForecastViewHolder holder, int position) {
        ForecastInfo info = mForecastInfo.get(position);
        holder.setDay(info.day);
        holder.setWeatherDesc(info.weatherDesc);
        holder.setLowHigh(info.lowHigh);
        holder.setIcon(info.iconUrl, mContext);

        holder.setPosition(position);

        if(mOrientation == Configuration.ORIENTATION_LANDSCAPE){
            holder.setDetailFullDesc(info.formalDesc);
            holder.setDetailHumidity("" + info.humidity);
            holder.setDetailRain("" + info.rainIn);
            holder.setDetailAvgWind("" + info.aveWind);
            holder.setDetailMaxWind("" + info.maxWind);
            holder.setDetailWindDir("" + info.windDir);
            holder.setDetailSnow("" +info.snowIn);

            String low = info.lowHigh.substring(0, info.lowHigh.indexOf('/'));
            String high = info.lowHigh.substring(info.lowHigh.indexOf('/') + 1);

            holder.setDetailLow(low);
            holder.setDetailHigh(high);
        }

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
