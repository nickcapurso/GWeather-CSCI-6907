package io.capurso.gweather.weather.forecast;

import android.content.Context;
import android.content.res.Configuration;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import io.capurso.gweather.R;

public class ForecastViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
    private static final String TAG = ForecastViewHolder.class.getName();
    private View mContainer;
    private TextView mTvDay, mTvWeatherDesc, mTvLowHigh;
    private ImageView mIvWeatherIcon;

    private int mPosition;
    private ForecastViewClickListener mListener;

    private TextView mDetailLow, mDetailHigh, mDetailHumidity,
        mDetailRain, mDetailAvgWind, mDetailMaxWind, mDetailWindDir, mDetailSnow,
        mDetailFullDesc;


    public ForecastViewHolder(View view, int orientation, ForecastViewClickListener listener) {
        super(view);

        mListener = listener;
        mContainer = view;
        mTvDay = (TextView) view.findViewById(R.id.tvDay);
        mTvWeatherDesc = (TextView) view.findViewById(R.id.tvWeatherDesc);
        mTvLowHigh = (TextView) view.findViewById(R.id.tvLowHigh);
        mIvWeatherIcon = (ImageView) view.findViewById(R.id.ivWeatherIcon);


        if(orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setupDetailWidgets(view);
            view.setClickable(false);
        }else{
            view.setOnClickListener(this);
        }
    }

    private void setupDetailWidgets(View view){
        mDetailFullDesc = (TextView)view.findViewById(R.id.detail_full_desc);
        mDetailLow = (TextView)view.findViewById(R.id.value_detail_low);
        mDetailHigh = (TextView)view.findViewById(R.id.value_detail_high);
        mDetailHumidity = (TextView)view.findViewById(R.id.value_detail_humidity);
        mDetailRain = (TextView)view.findViewById(R.id.value_detail_rain);
        mDetailAvgWind = (TextView)view.findViewById(R.id.value_detail_avgwind);
        mDetailMaxWind = (TextView)view.findViewById(R.id.value_detail_maxwind);
        mDetailWindDir = (TextView)view.findViewById(R.id.value_detail_winddir);
        mDetailSnow = (TextView)view.findViewById(R.id.value_detail_snow);

        LinearLayout detailHeader = (LinearLayout)view.findViewById(R.id.detailHeader);
        detailHeader.setVisibility(View.GONE);

        View shadowView = (View)view.findViewById(R.id.viewShadow);
        shadowView.setVisibility(View.GONE);
    }

    public void setPosition(int i){
        mPosition = i;
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

    public void setIcon(String url, Context context){
        Picasso.with(context).load(url).into(mIvWeatherIcon);
    }

    public View getContainer() {
        return mContainer;
    }

    public void setDetailLow(String detailLow) {
        mDetailLow.setText(detailLow);
    }

    public void setDetailHigh(String detailHigh) {
        mDetailHigh.setText(detailHigh);
    }

    public void setDetailHumidity(String detailHumidity) {
        mDetailHumidity.setText(detailHumidity);
    }

    public void setDetailRain(String detailRain) {
        mDetailRain.setText(detailRain);
    }

    public void setDetailAvgWind(String detailAvgWind) {
        mDetailAvgWind.setText(detailAvgWind);
    }

    public void setDetailMaxWind(String detailMaxWind) {
        mDetailMaxWind.setText(detailMaxWind);
    }

    public void setDetailWindDir(String detailWindDir) {
        mDetailWindDir.setText(detailWindDir);
    }

    public void setDetailSnow(String detailSnow) {
        mDetailSnow.setText(detailSnow);
    }

    public void setDetailFullDesc(String detailFullDesc) {
        mDetailFullDesc.setText(detailFullDesc);
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "Forecast clicked = " + mPosition);
        mListener.onForecastViewClicked(mPosition);
    }
}