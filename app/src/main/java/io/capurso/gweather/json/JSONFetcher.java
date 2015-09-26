package io.capurso.gweather.json;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Class used to execute a network request in the from of an HTTP GET message. The class is named
 * since all APIs used return JSON objects.
 */
public class JSONFetcher extends AsyncTask<String, Void, String> {
    private static final String TAG = "JSONFetcher";
    private JSONEventListener mClientListener;
    private NetworkTimeout mTimer;

    public JSONFetcher(JSONEventListener client){
        mClientListener = client;
    }

    /**
     * Begin a network timeout timer which will alert the caller's handler if the timeout expires
     */
    @Override
    protected void onPreExecute() {
        mTimer = new NetworkTimeout(mClientListener);
        mTimer.start();
    }

    /**
     * Uses apache.http classes to build and execute a network request
     * @param params An array of key-value pairs to include into the URL (for example,
     *               for the Geocoding API, there will be "address" following by the address to geocode.)
     *               Odd-indexed parameters are the "keys" and the even-indexed parameters are the "values"
     *
     *               The first param (params[0]) will be the URL up until the optional components.
     *
     *               Example:
     *               param[0] = "http://maps.google.com/maps/api/geocode/json"
     *               param[1] = "address"
     *               param[2] = "123 Main Street ..."
     * @return "err" or "okay"
     */
    @Override
    protected String doInBackground(String... params) {
        Log.d(TAG, "JSONFetcher starting...");
        HttpClient httpclient = HttpClients.createDefault();

        try {
            URIBuilder builder = new URIBuilder(params[0]);

            //Set params (odd params are the keys, even params are the values)
            for(int i = 1; i < params.length; i+=2)
                builder.setParameter(params[i], params[i+1]);

            URI uri = builder.build();
            HttpGet request = new HttpGet(uri);
            HttpResponse response = httpclient.execute(request);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                //TODO
                return (EntityUtils.toString(entity));
            }
        } catch (ClientProtocolException e) {
            Log.d(TAG, "JSONFetcher - ClientProtocolException " + e.getStackTrace());
            return "err";
        } catch (IOException e) {
            Log.d(TAG, "JSONFetcher - IOException " + e.getStackTrace());
            return "err";
        } catch (URISyntaxException e) {
            Log.d(TAG, "JSONFetcher - URISyntaxException " + e.getStackTrace());
            return "err";
        }
        return "okay";
    }

    @Override
    protected void onPostExecute(String result) {
        mTimer.cancel();
        //Send the handler code for JSON fetching errors
        if(result.equals("err")){
            mClientListener.onJSONFetchErr();

        //Send a handler message indicated that the fetch was successful and include the JSON result
        //as the message's contained object.
        }else {
            Log.d(TAG, "Result: " + result);
            mClientListener.onJSONFetchSuccess(result);
        }
    }
}
