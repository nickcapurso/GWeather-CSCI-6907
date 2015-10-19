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

import io.capurso.gweather.common.Timeout;
import io.capurso.gweather.common.TimeoutListener;
import static io.capurso.gweather.common.Utils.DEBUG;


/**
 * Class used to execute a network request in the from of an HTTP GET message. The class is named
 * since all APIs used return JSON objects.
 */
public class JSONFetcher extends AsyncTask<String, Void, String> implements TimeoutListener {
    private static final String TAG = JSONFetcher.class.getName();

    /**
     * Indicates a successful network query. Used by doInBackground.
     */
    private static final String RESULT_GOOD = "success";

    /**
     * Indicates a failed network query. Used by doInBackground.
     */
    private static final String RESULT_ERR = "error";

    /**
     * Ignores a returning network request if the timeout expires.
     */
    private boolean mTimeoutOccurred;

    /**
     * The "client" listener to send callbacks to.
     */
    private JSONEventListener mClientListener;

    /**
     * Internally uses a Timeout instance to determine when a network request is taking
     * too long.
     */
    private Timeout mTimer;

    /**
     * Default constructor - takes in a listener for callbacks.
     * @param client For result callbacks.
     */
    public JSONFetcher(JSONEventListener client){
        mClientListener = client;
    }

    /**
     * Before making the network request, start the countdown timer.
     */
    @Override
    protected void onPreExecute() {
        mTimer = new Timeout(this);
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
     * @return Returns the retrieved data on success, or an error code on failure.
     */
    @Override
    protected String doInBackground(String... params) {
        if(DEBUG) Log.d(TAG, "JSONFetcher starting...");
        HttpClient httpclient = HttpClients.createDefault();

        try {
            URIBuilder builder = new URIBuilder(params[0]);

            //Set params (odd params are the keys, even params are the values)
            for(int i = 1; i < params.length; i+=2)
                builder.setParameter(params[i], params[i+1]);

            URI uri = builder.build();
            if(DEBUG) Log.d(TAG, "JSONFetecher requesting: " + uri.toString());
            HttpGet request = new HttpGet(uri);
            HttpResponse response = httpclient.execute(request);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                return (EntityUtils.toString(entity));
            }
        } catch (ClientProtocolException e) {
            if(DEBUG) Log.d(TAG, "JSONFetcher - ClientProtocolException " + e.getMessage());
            return RESULT_ERR;
        } catch (IOException e) {
            if(DEBUG) Log.d(TAG, "JSONFetcher - IOException " + e.getMessage());
            return RESULT_ERR;
        } catch (URISyntaxException e) {
            if(DEBUG) Log.d(TAG, "JSONFetcher - URISyntaxException " + e.getMessage());
            return RESULT_ERR;
        }
        return RESULT_ERR;
    }

    @Override
    protected void onPostExecute(String result) {
        //Ignore the result if a timeout had occurred.
        if(mTimeoutOccurred)
            return;

        //Otherwise, cancel the timer.
        mTimer.cancel();

        if(DEBUG) Log.d(TAG, "Result: " + result);

        //If there was an error, notify the client via the interface. Otherwise, send them
        //the JSON data contained in a String.
        if(result.equals(RESULT_ERR))
            mClientListener.onJSONFetchErr();
        else
            mClientListener.onJSONFetchSuccess(result);
    }

    /**
     * If a timeout occurs, notify the client via the interface.
     */
    @Override
    public void onTimeout() {
        mTimeoutOccurred = true;
        mClientListener.onNetworkTimeout();
    }
}
