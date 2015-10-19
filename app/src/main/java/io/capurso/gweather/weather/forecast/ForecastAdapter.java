package io.capurso.gweather.weather.forecast;

import android.content.Context;
import android.content.res.Configuration;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import java.util.List;

import io.capurso.gweather.R;

import static io.capurso.gweather.common.Utils.DEBUG;
/**
 * Extended Adapter for ForecastInfo to display in the RecyclerView. Following the
 * ViewHolder pattern.
 */
public class ForecastAdapter extends RecyclerView.Adapter<ForecastViewHolder> {
    private static final String TAG = ForecastAdapter.class.getName();

    /**
     * The list of objects to display in the RecyclerView
     */
    private List<ForecastInfo> mForecastInfo;

    /**
     * Activity context.
     */
    private Context mContext;

    /**
     * Keep track of the last item position that was animated (so we only animate new items).
     */
    private int mLastAnimated = -1;

    /**
     * The current orientation of the device. The RecyclerView's items will look different
     * based on screen rotation.
     */
    private int mOrientation;

    /**
     * A listener for item click events.
     */
    private ForecastViewClickListener mListener;

    /**
     * @param context Activity context for display ImageViews and filling them using Picasso.
     * @param info The list of ForecastInfo items to display.
     * @param listener A listener for item click events.
     */
    public ForecastAdapter(Context context, List<ForecastInfo> info, ForecastViewClickListener listener){
        mContext = context;
        mForecastInfo = info;
        mOrientation = context.getResources().getConfiguration().orientation;
        mListener = listener;
    }

    /**
     * Inflate the row item layout file for each new list item. Under the ViewHolder pattern,
     * each ViewHolder keeps a reference to the inflated view, so it does not need to occur
     * when redrawing.
     *
     * Only called once per list item.
     * @param parent
     * @param viewType
     * @return A newly created ViewHolder for the inflated view
     */
    @Override
    public ForecastViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_forecast, parent, false);
        if(DEBUG) Log.d(TAG, "onCreateViewHolder");
        return new ForecastViewHolder(v, mOrientation, mListener);
    }

    /**
     * Called whenever a list item is "redrawn" on the screen (i.e. when the user scrolls up or down).
     * However, because of the ViewHolder pattern, we don't have to do any findViewById calls (since
     * the ViewHolder already has a reference to each of its children).
     * @param holder The ViewHolder to redraw.
     * @param position Using information at what index in the ForecastInfo array.
     */
    @Override
    public void onBindViewHolder(ForecastViewHolder holder, int position) {
        ForecastInfo info = mForecastInfo.get(position);
        holder.setDay(info.day);
        holder.setWeatherDesc(info.weatherDesc);
        holder.setLowHigh(info.lowHigh);
        holder.setIcon(info.iconUrl, mContext);

        if(DEBUG) Log.d(TAG, "onBindViewHolder");
        holder.setPosition(position);

        //We display more detailed information if the user is in landscape mode
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

        //Animate this view if it is the first time it is being seen.
        if(position > mLastAnimated) {
            holder.getContainer().startAnimation(AnimationUtils.loadAnimation(mContext, android.R.anim.slide_in_left));
            mLastAnimated = position;
        }
    }

    /**
     * Used the reset the animation count (for example, if we clear the forecast list).
     */
    public void resetAnimationCount(){
        mLastAnimated = -1;
    }

    @Override
    public int getItemCount() {
        return mForecastInfo.size();
    }
}
