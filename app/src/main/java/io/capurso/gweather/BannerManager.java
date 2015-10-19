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
 * Manages the "banner" at the top of the ForecastActivity in portrait mode. This includes
 * the FrameLayout containing the location's image, the current temperature, and the
 * description of the location (ex. "New York, NY 10001, USA")
 *
 * The image is loaded via Picasso.
 */
public class BannerManager implements JSONEventListener{
    private static final String TAG = BannerManager.class.getName();

    /**
     * Context for Picasso.
     */
    private Context mContext;

    /**
     * ImageView for loading in an image of the current location.
     */
    private ImageView mIvBanner;

    /**
     * TextView for displaying the current temperature. Overlays the banner image.
     */
    private TextView mTvCurrTemp;

    /**
     * Takes in Context, the ImageView to load the banner image into, and the the TextView
     * to place the current temperature.
     * @param context
     * @param iv
     * @param tv
     */
    public BannerManager(Context context, ImageView iv, TextView tv){
        mContext = context;
        mIvBanner = iv;
        mTvCurrTemp = tv;
    }

    /**
     * Takes the textual description of a location from a LocationWrapper and starts
     * a Google Images query.
     * @param wrapper
     */
    public void setupBanner(LocationWrapper wrapper){
        new JSONFetcher(this).execute(API_URLS.GOOGLE_IMAGES, API_URLS.GOOGLE_IMAGES_QUERY, wrapper.searchString,
                API_URLS.GOOGLE_IMAGES_RESPONSES, "6"); //Get a max of 6 results
    }

    /**
     * Sets the current temperature TextView with the supplied String.
     * @param currTemp
     */
    public void setCurrentTemp(String currTemp){
        mTvCurrTemp.setVisibility(View.VISIBLE);
        mTvCurrTemp.setText(currTemp);
    }

    /**
     * Hides the current temperature view.
     */
    public void hideCurrentTemp(){
        mTvCurrTemp.setVisibility(View.GONE);
    }

    /**
     * Causes the banner image to be shown (when it was previously invisible waiting for an image).
     * Also sets the proper height.
     */
    private void showImageView(){
        if(mIvBanner.getVisibility() != View.VISIBLE) {
            mIvBanner.setVisibility(View.VISIBLE);
            ViewGroup.LayoutParams params = mIvBanner.getLayoutParams();
            params.height = (int) mContext.getResources().getDimension(R.dimen.banner_height);
            mIvBanner.setLayoutParams(params);
        }
    }

    /**
     * If there is an error retrieving the image. Instead of showing a blank hole, use the
     * placeholder image.
     */
    private void showErrPlaceholder(){
        mIvBanner.setBackgroundResource(R.drawable.placeholder);
        showImageView();
    }

    /**
     * Network error retrieving the image.
     */
    @Override
    public void onNetworkTimeout() {
        showErrPlaceholder();
    }

    /**
     * Network error retrieving the image.
     */
    @Override
    public void onJSONFetchErr() {
        showErrPlaceholder();
    }

    /**
     * On success, we will have a list of image results to choose from. Uses the first image that
     * is not from a Wikipedia site (usually very large and troublesome to download) and is
     * also not behind HTTPS.
     * @param result JSON string containing a list of search results.
     */
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

                if(!imgUrl.contains("https") && !imgUrl.contains("wiki")) //Skip Wiki and HTTPS images
                    break;
            }

            if(DEBUG) Log.d(TAG, "Image chosen: " + imgUrl);

            showImageView();

            //Place the image into the banner ImageView using Picasso
            Picasso.with(mContext).load(imgUrl).placeholder(R.drawable.placeholder).into(mIvBanner, new Callback() {
                @Override
                public void onSuccess() { }

                @Override
                public void onError() {
                    if(DEBUG) Log.d(TAG, "Picasso failed to get image");
                    showErrPlaceholder(); //Network error
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
