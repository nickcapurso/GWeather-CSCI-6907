package io.capurso.gweather;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.capurso.gweather.common.API_URLS;
import io.capurso.gweather.json.JSONEventListener;
import io.capurso.gweather.json.JSONFetcher;
import io.capurso.gweather.location.LocationWrapper;

import static io.capurso.gweather.common.Utils.DEBUG;

/**
 * Created by Nick on 10/5/15.
 */
public class BannerManager implements JSONEventListener{
    private static final String TAG = BannerManager.class.getName();

    private Context mContext;
    private ImageView mIvBanner;
    private TextView mTvCurrTemp;

    public BannerManager(Context context, ImageView iv, TextView tv){
        mContext = context;
        mIvBanner = iv;
        mTvCurrTemp = tv;
    }

    public void setupBanner(LocationWrapper wrapper){
        new JSONFetcher(this).execute(API_URLS.GOOGLE_IMAGES, API_URLS.GOOGLE_IMAGES_QUERY, wrapper.searchString,
                API_URLS.GOOGLE_IMAGES_RESPONSES, "6");
    }

    public void setCurrentTemp(String currTemp){
        mTvCurrTemp.setVisibility(View.VISIBLE);
        mTvCurrTemp.setText(currTemp);
    }

    public void hideCurrentTemp(){
        mTvCurrTemp.setVisibility(View.GONE);
    }

    private void showImageView(){
        if(mIvBanner.getVisibility() != View.VISIBLE) {
            mIvBanner.setVisibility(View.VISIBLE);
            ViewGroup.LayoutParams params = mIvBanner.getLayoutParams();
            params.height = (int) mContext.getResources().getDimension(R.dimen.banner_height);
            mIvBanner.setLayoutParams(params);
        }
    }

    private void showErrPlaceholder(){
        mIvBanner.setBackgroundResource(R.drawable.placeholder);
        showImageView();
    }
    @Override
    public void onNetworkTimeout() {
        showErrPlaceholder();
    }

    @Override
    public void onJSONFetchErr() {
        showErrPlaceholder();
    }

    @Override
    public void onJSONFetchSuccess(String result) {
        try {
            JSONObject topObj = new JSONObject(result);
            JSONArray imageArr = topObj.getJSONObject("responseData").getJSONArray("results");

            String imgUrl = "";
            int i = 0;

            while(i < imageArr.length()){
                imgUrl = imageArr.getJSONObject(i++).getString("url");
                if(DEBUG) Log.d(TAG, "Trying URL: " + imgUrl);

                if(!imgUrl.contains("https") && !imgUrl.contains("wiki"))
                    break;
            }

            if(DEBUG) Log.d(TAG, "Image chosen: " + imgUrl);

            showImageView();

            Picasso.with(mContext).load(imgUrl).placeholder(R.drawable.placeholder).into(mIvBanner, new Callback() {
                @Override
                public void onSuccess() { }

                @Override
                public void onError() {
                    if(DEBUG) Log.d(TAG, "Picasso failed to get image");
                    showErrPlaceholder();
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
