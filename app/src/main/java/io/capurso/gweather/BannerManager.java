package io.capurso.gweather;

import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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
    private ImageView mImageView;

    public BannerManager(Context context, ImageView view){
        mContext = context;
        mImageView = view;
    }

    public void setupBanner(LocationWrapper wrapper){
        if(mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            return;

        new JSONFetcher(this).execute(API_URLS.GOOGLE_IMAGES, API_URLS.GOOGLE_IMAGES_QUERY, wrapper.searchString,
                API_URLS.GOOGLE_IMAGES_RESPONSES, "6");
    }

    @Override
    public void onNetworkTimeout() {

    }

    @Override
    public void onJSONFetchErr() {

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

            Picasso.with(mContext).load(imgUrl).placeholder(R.drawable.placeholder).into(mImageView);

            mImageView.setVisibility(View.VISIBLE);
            ViewGroup.LayoutParams params = mImageView.getLayoutParams();
            params.height = (int)mContext.getResources().getDimension(R.dimen.banner_height);
            mImageView.setLayoutParams(params);


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
