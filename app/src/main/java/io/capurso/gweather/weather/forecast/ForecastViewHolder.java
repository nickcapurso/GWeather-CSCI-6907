package io.capurso.gweather.weather.forecast;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import io.capurso.gweather.R;


public class ForecastViewHolder extends RecyclerView.ViewHolder{
    private View mContainer;
    private TextView mTvDay, mTvWeatherDesc, mTvLowHigh, mTvCurrTemp;
    private ImageView mIvWeatherIcon;

    public ForecastViewHolder(View view) {
        super(view);

        mContainer = view;
        mTvDay = (TextView) view.findViewById(R.id.tvDay);
        mTvWeatherDesc = (TextView) view.findViewById(R.id.tvWeatherDesc);
        mTvLowHigh = (TextView) view.findViewById(R.id.tvLowHigh);
        mTvCurrTemp = (TextView) view.findViewById(R.id.tvCurrentTemp);
        mIvWeatherIcon = (ImageView) view.findViewById(R.id.ivWeatherIcon);
    }

    public void setDay(String day){
        mTvDay.setText(day);
    }

    public void setWeatherDesc(String weather){
        mTvWeatherDesc.setText(weather);
    }

    public void setLowHigh(String lowHigh){
        mTvLowHigh.setText(lowHigh);
    }

    public void setCurrTemp(String temp){
        mTvCurrTemp.setText(temp);
    }

    public void setIcon(String url, Context context){
        //mIvWeatherIcon.setImageResource(id);
        Picasso.with(context).load(url).into(mIvWeatherIcon);
    }

    public View getContainer() {
        return mContainer;
    }
}